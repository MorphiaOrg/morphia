package dev.morphia.audits.shell

import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.HexFormat
import org.bson.*
import org.bson.codecs.BsonValueCodec
import org.bson.codecs.DecoderContext
import org.bson.json.JsonParseException
import org.bson.json.JsonReader

/** Explicit legacy fixture data format, not JavaScript evaluation or inferred shell semantics. */
data class LegacyDocumentsResult(
    val state: ShellLiteralState,
    val source: String,
    val sourceSha256: String?,
    val documents: List<BsonDocument>,
    val problems: List<ShellLiteralProblem>,
)

object LegacyDocuments {
    /**
     * Union of document streams and comma-separated document lists/one array of documents. Both
     * maintained-parser interpretations must agree if both accept. No delimiters are inserted
     * between source tokens and no code/string contents are rewritten.
     */
    fun parse(
        source: String,
        limits: ShellLiteralLimits = ShellLiteralLimits(),
    ): LegacyDocumentsResult {
        var hash: String? = null
        fun failed(state: ShellLiteralState, code: String, message: String) =
            LegacyDocumentsResult(
                state,
                source,
                hash,
                emptyList(),
                listOf(ShellLiteralProblem(code, message, null)),
            )
        if (source.length > limits.maximumCharacters)
            return failed(
                ShellLiteralState.INVALID,
                "SOURCE_LIMIT",
                "Legacy document source exceeds character limit",
            )
        try {
            val bytes = utf8(source)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            hash = HexFormat.of().formatHex(digest.digest())
        } catch (failure: CharacterCodingException) {
            return failed(
                ShellLiteralState.INVALID,
                "SOURCE_ENCODING",
                "Invalid source UTF-8 identity",
            )
        }
        val accepted = mutableListOf<List<BsonDocument>>()
        val errors = mutableListOf<ShellLiteralProblem>()
        for (framed in listOf(false, true)) {
            try {
                val roots = Reader(limits).read(if (framed) "[" + source + "\n]" else source)
                val values =
                    if (framed) {
                        if (roots.size != 1 || roots.single() !is BsonArray)
                            throw InvalidData("STREAM_FRAME", "Invalid legacy list envelope")
                        roots.single().asArray().values
                    } else roots
                if (values.isEmpty())
                    throw InvalidData("EMPTY_INPUT", "No document value was provided")
                val documents =
                    if (values.size == 1 && values.single() is BsonArray)
                        values.single().asArray().values
                    else values
                if (documents.any { it !is BsonDocument })
                    throw InvalidData(
                        "DOCUMENT_REQUIRED",
                        "Legacy fixture input must contain documents",
                    )
                accepted += documents.map { it.asDocument() }
            } catch (failure: InvalidData) {
                errors += ShellLiteralProblem(failure.code, failure.message ?: failure.code, null)
            } catch (failure: JsonParseException) {
                errors +=
                    ShellLiteralProblem(
                        "LEGACY_SYNTAX",
                        failure.message ?: "Invalid BSON JSON syntax",
                        null,
                    )
            } catch (failure: BSONException) {
                errors +=
                    ShellLiteralProblem(
                        "LEGACY_SYNTAX",
                        failure.message ?: "Invalid BSON JSON syntax",
                        null,
                    )
            } catch (failure: NumberFormatException) {
                errors +=
                    ShellLiteralProblem(
                        "LEGACY_NUMBER",
                        "Invalid BSON JSON numeric representation",
                        null,
                    )
            } catch (failure: IllegalArgumentException) {
                // JsonReader's ObjectId/base64 constructors use this documented Java failure type.
                errors +=
                    ShellLiteralProblem(
                        "LEGACY_SYNTAX",
                        "Invalid BSON JSON constructor value",
                        null,
                    )
            }
        }
        if (accepted.isEmpty())
            return LegacyDocumentsResult(
                ShellLiteralState.UNSUPPORTED,
                source,
                hash,
                emptyList(),
                errors.distinct(),
            )
        if (
            accepted.drop(1).any { candidate ->
                BsonComparison.compare(BsonArray(accepted.first()), BsonArray(candidate)).state !=
                    BsonComparisonState.EQUAL
            }
        )
            return failed(
                ShellLiteralState.UNSUPPORTED,
                "AMBIGUOUS_STREAM",
                "Legacy input has conflicting interpretations",
            )
        return LegacyDocumentsResult(
            ShellLiteralState.NORMALIZED,
            source,
            hash,
            accepted.first(),
            emptyList(),
        )
    }

    private fun utf8(value: String) =
        Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(value))

    private class InvalidData(val code: String, message: String) : RuntimeException(message)

    private class Reader(val limits: ShellLiteralLimits) {
        private var nodes = 0

        fun read(source: String): List<BsonValue> =
            JsonReader(source).use { reader ->
                val values = mutableListOf<BsonValue>()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) values += value(reader, 0)
                values
            }

        private fun take(depth: Int) {
            if (++nodes > limits.maximumNodes || depth > limits.maximumDepth)
                throw InvalidData("VALUE_LIMIT", "Legacy values exceed node/depth limit")
        }

        private fun text(value: String, cstring: Boolean = false): String {
            if (cstring && '\u0000' in value)
                throw InvalidData("BSON_CSTRING", "NUL is not a BSON cstring")
            try {
                utf8(value)
            } catch (failure: CharacterCodingException) {
                throw InvalidData("BSON_STRING_ENCODING", "Decoded value has an unpaired surrogate")
            }
            return value
        }

        private fun document(reader: BsonReader, depth: Int): BsonDocument {
            take(depth)
            reader.readStartDocument()
            val result = BsonDocument()
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                val name = text(reader.readName(), true)
                if (result.containsKey(name))
                    throw InvalidData("DUPLICATE_KEY", "Duplicate BSON field: $name")
                result[name] = value(reader, depth + 1)
            }
            reader.readEndDocument()
            return result
        }

        private fun value(reader: BsonReader, depth: Int): BsonValue {
            if (reader.currentBsonType == BsonType.DOCUMENT) return document(reader, depth)
            take(depth)
            if (reader.currentBsonType == BsonType.ARRAY) {
                reader.readStartArray()
                val result = BsonArray()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) result.add(
                    value(reader, depth + 1)
                )
                reader.readEndArray()
                return result
            }
            if (reader.currentBsonType == BsonType.JAVASCRIPT_WITH_SCOPE) {
                val code = text(reader.readJavaScriptWithScope())
                return BsonJavaScriptWithScope(code, document(reader, depth + 1))
            }
            val scalar = BsonValueCodec().decode(reader, DecoderContext.builder().build())
            when (scalar.bsonType) {
                BsonType.STRING -> text(scalar.asString().value)
                BsonType.SYMBOL -> text(scalar.asSymbol().symbol)
                BsonType.JAVASCRIPT -> text(scalar.asJavaScript().code)
                BsonType.DB_POINTER -> text(scalar.asDBPointer().namespace, true)
                BsonType.REGULAR_EXPRESSION -> {
                    text(scalar.asRegularExpression().pattern, true)
                    text(scalar.asRegularExpression().options, true)
                }
                else -> Unit
            }
            return scalar
        }
    }
}
