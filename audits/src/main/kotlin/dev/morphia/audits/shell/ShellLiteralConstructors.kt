package dev.morphia.audits.shell

import java.nio.ByteBuffer
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.HexFormat
import java.util.Locale
import java.util.UUID
import org.bson.*
import org.bson.types.Decimal128
import org.bson.types.ObjectId

/** Closed data constructors, not a JS runtime or a general constant evaluator. */
internal object ShellLiteralConstructors {
    private val explicitIsoDate =
        Regex(
            "(?:[0-9]{4}|[+-][0-9]{6})-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2})"
        )
    // mongosh and the legacy shell parse ISODate's omitted time as midnight and its omitted offset
    // as UTC, and accept an offset without its colon, but only after a time. Compact and hour-only
    // forms are not reviewed.
    private val shellIsoDate =
        Regex(
            "([0-9]{4}-[0-9]{2}-[0-9]{2})(?:T([0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?)(?:(Z)|([+-][0-9]{2}):?([0-9]{2}))?)?"
        )
    // ECMAScript reads a date-only string as UTC midnight; a date-time without offset is local.
    private val dateOnly = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    private val asciiInteger = Regex("[+-]?[0-9]+")
    private val finiteDecimal = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")
    private val canonicalLong = Regex("(?:\\+?0|[+-]?[1-9][0-9]*)")
    private val uuidPattern = Regex("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}")

    /**
     * [text] with the time and offset the shell would assume made explicit, where it assumes any.
     */
    private fun explicitDate(name: String, text: String): String {
        if (explicitIsoDate.matches(text)) return text
        if (name == "Date") return if (dateOnly.matches(text)) "${text}T00:00:00Z" else text
        val match = shellIsoDate.matchEntire(text) ?: return text
        val (date, time, utc, hours, minutes) = match.destructured
        val offset = if (utc.isNotEmpty() || hours.isEmpty()) "Z" else "$hours:$minutes"
        return "${date}T${time.ifEmpty { "00:00:00" }}$offset"
    }

    fun construct(
        name: String,
        isNew: Boolean,
        arguments: List<BsonValue>,
        span: ShellSpan?,
        dialect: ShellDialect,
    ): BsonValue {
        fun unsupported(code: String, message: String): Nothing =
            throw ShellLiterals.UnsupportedLiteral(code, message, span)
        fun arity(count: Int) {
            if (arguments.size != count)
                unsupported("CONSTRUCTOR_ARITY", "$name requires $count explicit arguments")
        }
        fun string(index: Int): String =
            (arguments.getOrNull(index) as? BsonString)?.value
                ?: unsupported("CONSTRUCTOR_TYPE", "$name requires a string at argument $index")
        // Catch documented representation errors only at explicit library parsing boundaries.
        // Exceptions in our own dispatch/conversion logic must remain visible as bugs.
        fun <T> representation(parse: () -> T): T =
            try {
                parse()
            } catch (failure: DateTimeParseException) {
                unsupported(
                    "CONSTRUCTOR_FORMAT",
                    "Date requires an explicit ISO instant with UTC offset",
                )
            } catch (failure: ArithmeticException) {
                unsupported("CONSTRUCTOR_RANGE", "Value exceeds supported range for $name")
            } catch (failure: IllegalArgumentException) {
                unsupported("CONSTRUCTOR_FORMAT", "Invalid literal representation for $name")
            }
        fun integer(value: BsonValue, min: Long, max: Long): Long {
            val number =
                (value as? BsonDouble)?.value
                    ?: unsupported("CONSTRUCTOR_TYPE", "$name requires primitive integer constants")
            if (
                !number.isFinite() ||
                    number != kotlin.math.floor(number) ||
                    number < min.toDouble() ||
                    number > max.toDouble() ||
                    kotlin.math.abs(number) > 9_007_199_254_740_991.0
            )
                unsupported(
                    "CONSTRUCTOR_RANGE",
                    "$name integer is outside its exact supported range",
                )
            return number.toLong()
        }
        fun intArgument(index: Int, min: Long, max: Long) = integer(arguments[index], min, max)
        fun longString(): Long {
            val text = string(0)
            if (!canonicalLong.matches(text))
                unsupported(
                    "CONSTRUCTOR_FORMAT",
                    "$name requires a canonical decimal integer string",
                )
            return representation { text.toLong() }
        }
        if (
            dialect != ShellDialect.MONGOSH &&
                name in setOf("Int32", "Long", "Decimal128", "Double", "BSONRegExp")
        )
            unsupported("CONSTRUCTOR_DIALECT", "$name requires the mongosh dialect")
        return when (name) {
            "NumberInt",
            "Int32" -> {
                arity(1)
                val value =
                    if (arguments[0] is BsonString) {
                        val text = string(0)
                        if (!asciiInteger.matches(text))
                            unsupported(
                                "CONSTRUCTOR_FORMAT",
                                "$name requires an ASCII integer string",
                            )
                        representation { text.toLong() }
                    } else intArgument(0, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                if (value !in Int.MIN_VALUE..Int.MAX_VALUE)
                    unsupported("CONSTRUCTOR_RANGE", "$name exceeds Int32 range")
                BsonInt32(value.toInt())
            }
            "NumberLong" -> {
                arity(1)
                if (arguments[0] !is BsonString && dialect != ShellDialect.LEGACY_MONGO)
                    unsupported(
                        "CONSTRUCTOR_TYPE",
                        "This normalizer requires NumberLong strings outside the reviewed legacy dialect",
                    )
                BsonInt64(
                    if (arguments[0] is BsonString) longString()
                    else intArgument(0, -9_007_199_254_740_991L, 9_007_199_254_740_991L)
                )
            }
            "Long" -> {
                arity(1)
                // js-bson's numeric constructor takes LOW bits, with the omitted HIGH bits 0.
                // NumberLong/fromNumber are different APIs. Never silently equate them.
                val value =
                    if (arguments[0] is BsonString) longString()
                    else
                        intArgument(0, -9_007_199_254_740_991L, 9_007_199_254_740_991L)
                            .toInt()
                            .toLong() and 0xffff_ffffL
                BsonInt64(value)
            }
            "NumberDecimal",
            "Decimal128" -> {
                arity(1)
                val text = string(0)
                if (text.length > 1024)
                    unsupported("CONSTRUCTOR_LIMIT", "Decimal representation exceeds parsing limit")
                if (text.any { it.code > 127 })
                    unsupported("CONSTRUCTOR_FORMAT", "Decimal representation must be ASCII")
                val decimal =
                    when (text.lowercase(Locale.ROOT)) {
                        "nan",
                        "+nan",
                        "-nan" -> Decimal128.NaN
                        "inf",
                        "+inf",
                        "infinity",
                        "+infinity" -> Decimal128.POSITIVE_INFINITY
                        "-inf",
                        "-infinity" -> Decimal128.NEGATIVE_INFINITY
                        else -> {
                            if (!finiteDecimal.matches(text))
                                unsupported(
                                    "CONSTRUCTOR_FORMAT",
                                    "Invalid ASCII decimal representation",
                                )
                            representation { Decimal128.parse(text) }
                        }
                    }
                BsonDecimal128(decimal)
            }
            "Double" -> {
                arity(1)
                val value =
                    (arguments[0] as? BsonDouble)?.value
                        ?: unsupported(
                            "CONSTRUCTOR_TYPE",
                            "Double requires a primitive numeric constant; string coercion is unsupported",
                        )
                BsonDouble(value)
            }
            "ObjectId" -> {
                arity(1)
                val text = string(0)
                BsonObjectId(representation { ObjectId(text) })
            }
            "ISODate",
            "Date" -> {
                arity(1)
                if (name == "Date" && !isNew)
                    unsupported(
                        "DATE_CALL",
                        "Date() returns runtime text; use explicit new Date or ISODate",
                    )
                if (name == "ISODate" && isNew)
                    unsupported("DATE_CALL", "Only direct ISODate calls are supported")
                val millis =
                    if (arguments[0] is BsonString) {
                        val text = explicitDate(name, string(0))
                        if (!explicitIsoDate.matches(text))
                            unsupported(
                                "CONSTRUCTOR_FORMAT",
                                "Date requires an explicit ISO time and minute-precision UTC offset",
                            )
                        val instant = representation { OffsetDateTime.parse(text).toInstant() }
                        if (instant.nano % 1_000_000 != 0)
                            unsupported(
                                "DATE_PRECISION",
                                "Sub-millisecond dates need a reviewed precision rule",
                            )
                        representation { instant.toEpochMilli() }
                    } else {
                        if (name != "Date")
                            unsupported(
                                "CONSTRUCTOR_TYPE",
                                "ISODate requires an explicit UTC date string",
                            )
                        intArgument(0, -8_640_000_000_000_000L, 8_640_000_000_000_000L)
                    }
                if (millis !in -8_640_000_000_000_000L..8_640_000_000_000_000L)
                    unsupported("CONSTRUCTOR_RANGE", "Date exceeds JavaScript time range")
                BsonDateTime(millis)
            }
            "Timestamp" -> {
                val time: Long
                val increment: Long
                if (arguments.size == 1 && arguments[0] is BsonDocument) {
                    val value = arguments[0].asDocument()
                    if (value.keys != setOf("t", "i"))
                        unsupported("TIMESTAMP_FIELDS", "Timestamp requires exactly t and i")
                    time = integer(value.getValue("t"), 0, 0xffff_ffffL)
                    increment = integer(value.getValue("i"), 0, 0xffff_ffffL)
                } else {
                    arity(2)
                    time = intArgument(0, 0, 0xffff_ffffL)
                    increment = intArgument(1, 0, 0xffff_ffffL)
                }
                if (time == 0L && increment == 0L)
                    unsupported("DYNAMIC_TIMESTAMP", "Zero timestamp may be generated server-side")
                BsonTimestamp(time.toInt(), increment.toInt())
            }
            "BinData",
            "HexData" -> {
                arity(2)
                val subtype = intArgument(0, 0, 255).toByte()
                val text = string(1)
                val bytes = representation {
                    if (name == "BinData") Base64.getDecoder().decode(text)
                    else HexFormat.of().parseHex(text)
                }
                BsonBinary(subtype, bytes)
            }
            "UUID" -> {
                arity(1)
                val value = string(0)
                if (!uuidPattern.matches(value))
                    unsupported("UUID_FORMAT", "UUID requires its full hyphenated representation")
                val uuid = representation { UUID.fromString(value) }
                BsonBinary(
                    4.toByte(),
                    ByteBuffer.allocate(16)
                        .putLong(uuid.mostSignificantBits)
                        .putLong(uuid.leastSignificantBits)
                        .array(),
                )
            }
            "RegExp",
            "BSONRegExp" -> {
                arity(2)
                val flags = string(1)
                val allowed = if (name == "BSONRegExp") "imsx" else "im"
                if (flags.any { it !in allowed } || flags.toSet().size != flags.length)
                    unsupported("REGEX_FLAGS", "Unsupported regex flags for $name")
                val pattern = string(0)
                if ('\u0000' in pattern)
                    unsupported("REGEX_ENCODING", "BSON regex patterns cannot contain NUL")
                if (name == "RegExp")
                    ShellRegularExpressions.javascript(pattern, flags, span, fromString = true)
                else BsonRegularExpression(pattern, flags)
            }
            else -> unsupported("UNKNOWN_CONSTRUCTOR", "Unrecognized BSON constructor: $name")
        }
    }
}
