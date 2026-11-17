package dev.morphia.audits.offline

import dev.morphia.audits.semantics.*
import dev.morphia.audits.shell.*
import org.bson.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LegacyDocumentsTest {
    @Test
    fun invalidNativeConstructorArgumentsProduceUnsupportedVerdicts() {
        for (source in
            listOf("{_id:ObjectId('zz')}", "{_id:{${'$'}oid:'zz'}}", "{b:BinData(0,'!!!')}")) {
            val result = LegacyDocuments.parse(source)
            assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
            assertTrue(result.documents.isEmpty())
            assertTrue(result.problems.any { it.code == "LEGACY_SYNTAX" })
        }
    }

    @Test
    fun framesBothLegacyDocumentSequencesWithoutScanningBraces() {
        val a = "{a:'https://host/a//b',b:'} {',c:[1,2]}"
        val b = "{a:'x:  y',b:'{',c:[2,1]}"
        val expected = listOf(BsonDocument.parse(a), BsonDocument.parse(b))
        for (source in
            listOf(a + "\n" + b, a + ",\n" + b, a + ",\n" + b + ",", "[" + a + "," + b + "]")) {
            val result = LegacyDocuments.parse(source)
            assertEquals(ShellLiteralState.NORMALIZED, result.state, result.toString())
            assertEquals(source, result.source)
            assertEquals(
                BsonComparisonState.EQUAL,
                BsonComparison.compareResults(expected, result.documents).state,
            )
        }
    }

    @Test
    fun explicitArrayAndWhitespaceOnlyInputHaveDifferentMeaning() {
        val empty = LegacyDocuments.parse("[]")
        assertEquals(ShellLiteralState.NORMALIZED, empty.state)
        assertTrue(empty.documents.isEmpty())
        for (source in listOf("", "   ", ",", "{a:1} garbage", "{a:1}, broken(")) {
            val result = LegacyDocuments.parse(source)
            assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
            assertTrue(result.documents.isEmpty())
        }
    }

    @Test
    fun duplicateKeysAndMalformedTailsCannotBecomePartialSuccess() {
        for (source in listOf("{a:1,a:2}", "{x:{a:1,a:2}}", "{a:1}\n{b:", "{a:1}, {b:2")) {
            val result = LegacyDocuments.parse(source)
            assertEquals(ShellLiteralState.UNSUPPORTED, result.state, result.toString())
            assertTrue(result.documents.isEmpty())
        }
    }

    @Test
    fun dataFormatAndShellProgramAreNotGuessedFromSuccessfulParsing() {
        val source = "{v:{\$numberLong:'1'}}"
        val legacy = LegacyDocuments.parse(source)
        assertEquals(BsonType.INT64, legacy.documents.single()["v"]!!.bsonType)
        val shell = ShellLiterals.normalize(source, ShellDialect.MONGOSH)
        assertEquals(BsonType.DOCUMENT, shell.value!!.asDocument()["v"]!!.bsonType)
        assertEquals(
            ShellLiteralState.UNSUPPORTED,
            LegacyDocuments.parse("db.items.find({a:1})").state,
        )
    }

    @Test
    fun limitsAndInvalidStringsAreExplicit() {
        assertEquals(
            ShellLiteralState.INVALID,
            LegacyDocuments.parse("{a:1}", ShellLiteralLimits(maximumCharacters = 2)).state,
        )
        assertEquals(
            ShellLiteralState.UNSUPPORTED,
            LegacyDocuments.parse("{a:{b:{c:1}}}", ShellLiteralLimits(maximumDepth = 1)).state,
        )
        for (source in listOf("{'a\\u0000':1}", "{a:'\\uD800'}")) assertEquals(
            ShellLiteralState.UNSUPPORTED,
            LegacyDocuments.parse(source).state,
        )
    }
}
