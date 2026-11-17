package dev.morphia.audits.offline

import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import dev.morphia.audits.shell.*
import java.nio.file.Files
import java.time.Instant
import org.bson.*
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShellLiteralsTest {
    private fun value(source: String, dialect: ShellDialect = ShellDialect.MONGOSH): BsonValue {
        val result = ShellLiterals.normalize(source, dialect)
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.toString())
        assertEquals(source, result.source)
        assertTrue(result.problems.isEmpty())
        assertEquals(64, result.sourceSha256!!.length)
        return result.value!!
    }

    private fun unsupported(
        source: String,
        code: String? = null,
        dialect: ShellDialect = ShellDialect.MONGOSH,
    ) {
        val result = ShellLiterals.normalize(source, dialect)
        assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
        assertNull(result.value)
        assertEquals(source, result.source)
        if (code != null) assertEquals(code, result.problems.single().code, result.toString())
        result.problems.forEach { p ->
            p.span?.let { assertTrue(it.start in 0..it.end && it.end <= source.length) }
        }
    }

    private fun same(
        source: String,
        expected: BsonValue,
        dialect: ShellDialect = ShellDialect.MONGOSH,
    ) {
        assertEquals(
            BsonComparisonState.EQUAL,
            BsonComparison.compare(expected, value(source, dialect)).state,
            source,
        )
    }

    @Test
    fun preservesStringsUrlsCommentsAndWhitespaceWithoutTextRewrites() {
        same(
            "{url:'https://host/a//b',text:'a:  b', note:'// not a comment'} // EOF",
            BsonDocument("url", BsonString("https://host/a//b"))
                .append("text", BsonString("a:  b"))
                .append("note", BsonString("// not a comment")),
        )
        same(
            "/* intro */ {a: 'first\\nsecond', b:'quote\\\' and \\\"', c:'😀'}",
            BsonDocument("a", BsonString("first\nsecond"))
                .append("b", BsonString("quote' and \""))
                .append("c", BsonString("😀")),
        )
    }

    @Test
    fun preservesBsonNumericTypesRatherThanRewritingConstructors() {
        same("NumberInt('42')", BsonInt32(42))
        same("NumberLong('9223372036854775807')", BsonInt64(Long.MAX_VALUE))
        same("NumberLong('-9223372036854775808')", BsonInt64(Long.MIN_VALUE))
        same("NumberDecimal('0.10')", BsonDecimal128(Decimal128.parse("0.10")))
        same("Decimal128('1.00')", BsonDecimal128(Decimal128.parse("1.00")))
        same("Double(1)", BsonDouble(1.0))
        same("Int32(-2)", BsonInt32(-2))
        same("Long(42)", BsonInt64(42))
    }

    @Test
    fun dialectControlsDefaultNumbersAndUndefined() {
        same("1", BsonInt32(1))
        same("1", BsonDouble(1.0), ShellDialect.LEGACY_MONGO)
        same("undefined", BsonNull.VALUE)
        same("undefined", BsonUndefined(), ShellDialect.LEGACY_MONGO)
        unsupported("1", "SHELL_DIALECT_REQUIRED", ShellDialect.UNSPECIFIED)
        unsupported("undefined", "SHELL_DIALECT_REQUIRED", ShellDialect.UNSPECIFIED)
        same("NumberInt(1)", BsonInt32(1), ShellDialect.UNSPECIFIED)
        same("NumberLong('1')", BsonInt64(1), ShellDialect.UNSPECIFIED)
        unsupported("Long(1)", "CONSTRUCTOR_DIALECT", ShellDialect.UNSPECIFIED)
    }

    @Test
    fun numericLiteralPolicyPreservesFractionsLargeNumbersAndSignedZero() {
        same("1.25", BsonDouble(1.25))
        same("2147483648", BsonDouble(2147483648.0))
        same("-2147483648", BsonInt32(Int.MIN_VALUE))
        same("-0", BsonDouble(-0.0))
        same("0x10", BsonInt32(16))
        same("1e2", BsonInt32(100))
        same("+Infinity", BsonDouble(Double.POSITIVE_INFINITY))
        same("-Infinity", BsonDouble(Double.NEGATIVE_INFINITY))
        same("NaN", BsonDouble(Double.NaN))
        unsupported("-NumberLong('1')", "COMPUTED_VALUE")
    }

    @Test
    fun integerConstructorsRejectOverflowPrecisionLossAndWrongTypes() {
        unsupported("NumberInt('2147483648')", "CONSTRUCTOR_RANGE")
        unsupported("NumberLong('9223372036854775808')", "CONSTRUCTOR_FORMAT")
        unsupported("NumberLong(1)", "CONSTRUCTOR_TYPE")
        same("NumberLong(1)", BsonInt64(1), ShellDialect.LEGACY_MONGO)
        unsupported("NumberLong(9007199254740992)", "CONSTRUCTOR_RANGE", ShellDialect.LEGACY_MONGO)
        unsupported("NumberInt(1.5)", "CONSTRUCTOR_RANGE")
        unsupported("NumberInt(true)", "CONSTRUCTOR_TYPE")
        unsupported("NumberDecimal(0.10)", "CONSTRUCTOR_TYPE")
        unsupported("NumberDecimal('not a decimal')", "CONSTRUCTOR_FORMAT")
        unsupported("NumberInt()", "CONSTRUCTOR_ARITY")
    }

    @Test
    fun datesAreExplicitTimeZoneIndependentAndBounded() {
        same(
            "ISODate('2020-01-02T03:04:05.006Z')",
            BsonDateTime(Instant.parse("2020-01-02T03:04:05.006Z").toEpochMilli()),
        )
        same("new Date(0)", BsonDateTime(0))
        same(
            "new Date('2020-01-02T04:04:05+01:00')",
            BsonDateTime(Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()),
        )
        unsupported("Date('2020-01-01T00:00:00Z')", "DATE_CALL")
        unsupported("new Date()", "CONSTRUCTOR_ARITY")
        unsupported("ISODate()", "CONSTRUCTOR_ARITY")
        unsupported("ISODate('2020-01-01T00:00:00')", "CONSTRUCTOR_FORMAT")
        unsupported("ISODate('2020-01-01T00:00:00.0001Z')", "DATE_PRECISION")
        unsupported("new Date(8640000000000001)", "CONSTRUCTOR_RANGE")
    }

    @Test
    fun objectIdsAndBinaryValuesKeepBytesAndSubtypes() {
        same(
            "ObjectId('507f1f77bcf86cd799439011')",
            BsonObjectId(ObjectId("507f1f77bcf86cd799439011")),
        )
        same(
            "new ObjectId('507f1f77bcf86cd799439011')",
            BsonObjectId(ObjectId("507f1f77bcf86cd799439011")),
        )
        same("BinData(128,'AAH/')", BsonBinary(128.toByte(), byteArrayOf(0, 1, -1)))
        same("HexData(0,'0001ff')", BsonBinary(0.toByte(), byteArrayOf(0, 1, -1)))
        same(
            "UUID('00112233-4455-6677-8899-aabbccddeeff')",
            BsonBinary(
                4.toByte(),
                java.util.HexFormat.of().parseHex("00112233445566778899aabbccddeeff"),
            ),
        )
        unsupported("ObjectId()", "CONSTRUCTOR_ARITY")
        unsupported("ObjectId('x')", "CONSTRUCTOR_FORMAT")
        unsupported("BinData(256,'AA==')", "CONSTRUCTOR_RANGE")
        unsupported("HexData(0,'0')", "CONSTRUCTOR_FORMAT")
        unsupported("UUID('1-1-1-1-1')", "UUID_FORMAT")
    }

    @Test
    fun timestampsUseUnsignedComponentsAndRejectDynamicDefaults() {
        same("Timestamp({t:4294967295,i:2147483648})", BsonTimestamp(-1, Int.MIN_VALUE))
        same("Timestamp(1,2)", BsonTimestamp(1, 2), ShellDialect.LEGACY_MONGO)
        same("Timestamp(1,2)", BsonTimestamp(1, 2))
        unsupported("Timestamp()")
        unsupported("Timestamp({t:1})", "TIMESTAMP_FIELDS")
        unsupported("Timestamp({t:-1,i:2})", "CONSTRUCTOR_RANGE")
        unsupported("Timestamp({t:0,i:0})", "DYNAMIC_TIMESTAMP")
    }

    @Test
    fun regexValuesPreservePatternsAndExplicitSupportedOptions() {
        same("/a\\/b/im", BsonRegularExpression("a\\/b", "im"))
        same("RegExp('a+','mi')", BsonRegularExpression("a+", "im"))
        same("BSONRegExp('a # comment','x')", BsonRegularExpression("a # comment", "x"))
        unsupported("/a/g", "REGEX_FLAGS")
        unsupported("RegExp('a','ii')", "REGEX_FLAGS")
    }

    @Test
    fun documentAndArrayOrderArePreservedAndDuplicateKeysNeverCollapse() {
        val result = value("{z:[2,1,1],a:{b:1,a:2}}").asDocument()
        assertEquals(listOf("z", "a"), result.keys.toList())
        assertEquals(listOf(2, 1, 1), result["z"]!!.asArray().map { it.asInt32().value })
        assertEquals(listOf("b", "a"), result["a"]!!.asDocument().keys.toList())
        unsupported("{a:1,'a':2}", "DUPLICATE_KEY")
        unsupported("{a:1,'\\u0061':2}", "DUPLICATE_KEY")
        unsupported("{__proto__:null}", "PROTOTYPE_KEY")
        unsupported("[1,,2]", "ARRAY_HOLE")
        same("[null]", BsonArray(listOf(BsonNull.VALUE)))
    }

    @Test
    fun computedKeysSpreadsMethodsAndUnknownBindingsAreExplicitlyUnsupported() {
        listOf(
                "{[x]:1}",
                "{...x}",
                "[...x]",
                "{get a(){return 1}}",
                "{a(){return 1}}",
                "unknown",
                "x.y",
                "1+2",
                "true?1:2",
                "function(){return 1}",
                "(()=>1)()",
                "NumberInt(NumberLong('1'))",
            )
            .forEach { unsupported(it) }
    }

    @Test
    fun arbitraryCallsNeverExecuteAndDoNotYieldPartialValues() {
        val marker = Files.createTempDirectory("shell-no-eval-").resolve("marker")
        try {
            val source =
                "{ok:1,bad:Java.type('java.nio.file.Files').createFile(Java.type('java.nio.file.Path').of('" +
                    marker.toString().replace("\\", "\\\\") +
                    "'))}"
            unsupported(source)
            assertFalse(Files.exists(marker))
            unsupported("({}).constructor.constructor('return process')()")
            unsupported("fetch('https://example.invalid')")
            unsupported("new ObjectId('507f1f77bcf86cd799439011') {a:1}")
        } finally {
            Files.deleteIfExists(marker)
            Files.deleteIfExists(marker.parent)
        }
    }

    @Test
    fun sourceSpansReferToTheOriginalBlockIncludingUnicodeAndLeadingComments() {
        val source = "/*😀*/ {a: unknown}"
        val result = ShellLiterals.normalize(source, ShellDialect.MONGOSH)
        val span = result.problems.single().span!!
        assertEquals("unknown", source.substring(span.start, span.end))
        assertEquals(source, result.source)
        assertEquals(ShellLiterals.PARSER, result.parser)
    }

    @Test
    fun whitespaceAndCommentsDoNotChangeMeaningButStringWhitespaceDoes() {
        val a = value("{a:[1,NumberDecimal('0.10')],b:'x  y'}")
        val b = value(" /*intro*/ { a : [ 1, /*n*/ NumberDecimal( '0.10' ) ], b : 'x  y' } // EOF")
        assertEquals(BsonComparisonState.EQUAL, BsonComparison.compare(a, b).state)
        val c = value("{a:[1,NumberDecimal('0.10')],b:'x y'}")
        assertEquals(BsonComparisonState.DIFFERENT, BsonComparison.compare(a, c).state)
    }

    @Test
    fun truncatedAndPlaceholderSyntaxIsNeverNormalized() {
        listOf("", "{a:", "[1,", "{<operator>:1}", "{a:1};db.items.drop()", "db.items.find({a:1})")
            .forEach { unsupported(it) }
    }

    @Test
    fun sourceAndAstLimitsFailExplicitlyWithoutPartialOutput() {
        val source =
            ShellLiterals.normalize(
                "'too long'",
                ShellDialect.MONGOSH,
                ShellLiteralLimits(maximumCharacters = 3),
            )
        assertEquals(ShellLiteralState.INVALID, source.state)
        assertNull(source.sourceSha256)
        assertNull(source.value)
        val deep =
            ShellLiterals.normalize(
                "[[[1]]]",
                ShellDialect.MONGOSH,
                ShellLiteralLimits(maximumDepth = 1),
            )
        assertEquals(ShellLiteralState.UNSUPPORTED, deep.state)
        assertEquals("VALUE_LIMIT", deep.problems.single().code)
        val wide =
            ShellLiterals.normalize(
                "[1,2,3]",
                ShellDialect.MONGOSH,
                ShellLiteralLimits(maximumNodes = 2),
            )
        assertEquals(ShellLiteralState.UNSUPPORTED, wide.state)
        assertEquals("VALUE_LIMIT", wide.problems.single().code)
    }

    @Test
    fun unpairedSurrogatesAreExplicitRatherThanSilentlyReplaced() {
        val invalidSource = ShellLiterals.normalize("'" + '\uD800' + "'", ShellDialect.MONGOSH)
        assertEquals(ShellLiteralState.INVALID, invalidSource.state)
        assertEquals("SOURCE_ENCODING", invalidSource.problems.single().code)
        unsupported("'\\uD800'", "BSON_STRING_ENCODING")
        unsupported("{'\\uD800':1}", "BSON_STRING_ENCODING")
        same("'\\uD83D\\uDE00'", BsonString("😀"))
    }

    @Test
    fun invalidRegexSyntaxAndCstringNulsCannotBecomeBsonValues() {
        unsupported("RegExp('[','i')", "REGEX_SYNTAX")
        unsupported("/(?i)a/", "REGEX_SYNTAX")
        unsupported("{'\\u0000':1}", "BSON_KEY_ENCODING")
        unsupported("BSONRegExp('\\u0000','i')", "REGEX_ENCODING")
        unsupported("RegExp('" + "(".repeat(65) + "a" + ")".repeat(65) + "','i')", "REGEX_LIMIT")
        unsupported("RegExp('" + "a".repeat(4097) + "','i')", "REGEX_LIMIT")
    }

    @Test
    fun emptyContainersAndTrailingCommasDoNotBecomeSparseArrays() {
        same("[]", BsonArray())
        same("{}", BsonDocument())
        same("[1,]", BsonArray(listOf(BsonInt32(1))))
        same("{a:[],}", BsonDocument("a", BsonArray()))
        unsupported("/a/s", "REGEX_FLAGS")
        same("BSONRegExp('a','s')", BsonRegularExpression("a", "s"))
        for (source in listOf("[1,,]", "[,1]", "[,]", "[,,]")) unsupported(source, "ARRAY_HOLE")
    }

    @Test
    fun mongoshLongNumericArgumentsRepresentLowBitsRatherThanFromNumberValues() {
        same("Long(-1)", BsonInt64(4294967295L))
        same("Long(2147483648)", BsonInt64(2147483648L))
        same("Long(4294967296)", BsonInt64(0))
        same("Long(-4294967297)", BsonInt64(4294967295L))
        same("Long('-1')", BsonInt64(-1))
        same("NumberLong('-1')", BsonInt64(-1))
        unsupported("NumberLong('00')", "CONSTRUCTOR_FORMAT")
        unsupported("Long('-0')", "CONSTRUCTOR_FORMAT")
    }

    @Test
    fun javaDoubleStringSyntaxIsNeverAcceptedAsShellNumericCoercion() {
        for (text in listOf("1d", "1f", "0x1p3", "", "0x10", "1.0")) unsupported(
            "Double('" + text + "')",
            "CONSTRUCTOR_TYPE",
        )
    }

    @Test
    fun unambiguousDoubleValuesDoNotNeedAShellDialect() {
        for (source in
            listOf(
                "Infinity",
                "-Infinity",
                "+Infinity",
                "NaN",
                "-NaN",
                "1.25",
                "-0",
                "2147483648",
            )) {
            val explicit = value(source, ShellDialect.MONGOSH)
            same(source, explicit, ShellDialect.UNSPECIFIED)
        }
        unsupported("1", "SHELL_DIALECT_REQUIRED", ShellDialect.UNSPECIFIED)
    }

    @Test
    fun diagnosticSpansCoverTheExactNestedExpression() {
        val cases =
            listOf(
                "{v:NumberInt(1.5)}" to "NumberInt(1.5)",
                "{v:/a/g}" to "/a/g",
                "{v:[1,,2]}" to "[1,,2]",
            )
        for ((body, expression) in cases) {
            val source = "/*😀 prefix*/ " + body
            val result = ShellLiterals.normalize(source, ShellDialect.UNSPECIFIED)
            assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
            val span = result.problems.single().span
            assertNotNull(span, result.toString())
            assertEquals(expression, source.substring(span!!.start, span.end), result.toString())
        }
        for (source in listOf("{v:-1}", "{v:- /*comment*/ 1}", "{v:-(1)}")) {
            val result = ShellLiterals.normalize(source, ShellDialect.UNSPECIFIED)
            assertEquals(ShellLiteralState.UNSUPPORTED, result.state)
            assertNull(
                result.problems.single().span,
                "Unary hints omit the operator and must not be invented",
            )
            assertEquals(source, result.source)
        }
    }

    @Test
    fun javaOnlyDateFormsAndLeapSecondNormalizationAreUnsupported() {
        for (text in
            listOf(
                "2016-12-31T23:59:60Z",
                "2020-01-01T00:00:00+01:00:30",
                "2020-01-01T00:00:00+00:00:00",
                "2020-02-30T00:00:00Z",
            )) unsupported("ISODate('" + text + "')", "CONSTRUCTOR_FORMAT")
    }

    @Test
    fun javascriptRegexSerializationCannotInventDotAllOrUnescapedSource() {
        unsupported("/a/s", "REGEX_FLAGS")
        unsupported("RegExp('a','s')", "REGEX_FLAGS")
        unsupported("/a/g", "REGEX_FLAGS")
        unsupported("RegExp('a/b','')", "REGEX_SOURCE_FORM")
        unsupported("RegExp('','')", "REGEX_SOURCE_FORM")
        unsupported("RegExp('a\\nb','')", "REGEX_SOURCE_FORM")
        unsupported("RegExp('a\\u2028b','')", "REGEX_SOURCE_FORM")
        same("BSONRegExp('a/b','s')", BsonRegularExpression("a/b", "s"))
    }

    @Test
    fun integerAndDecimalStringGrammarsRejectUnicodeDigits() {
        for (name in
            listOf(
                "NumberInt",
                "Int32",
                "NumberLong",
                "Long",
                "NumberDecimal",
                "Decimal128",
            )) for (text in listOf("٣", "３")) unsupported(
            name + "('" + text + "')",
            "CONSTRUCTOR_FORMAT",
        )
        unsupported("NumberDecimal('" + "0".repeat(1025) + "')", "CONSTRUCTOR_LIMIT")
    }

    @Test
    fun decimalSpecialValuesUseLocaleIndependentNativeBsonRepresentations() {
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            same("NumberDecimal('INF')", BsonDecimal128(Decimal128.POSITIVE_INFINITY))
            same("NumberDecimal('-Infinity')", BsonDecimal128(Decimal128.NEGATIVE_INFINITY))
            same("NumberDecimal('-NaN')", BsonDecimal128(Decimal128.NaN))
            same("NumberDecimal('1.00e1')", BsonDecimal128(Decimal128.parse("1.00e1")))
        } finally {
            java.util.Locale.setDefault(original)
        }
    }
}
