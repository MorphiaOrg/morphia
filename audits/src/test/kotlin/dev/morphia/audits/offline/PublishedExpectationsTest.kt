package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.published.*
import dev.morphia.audits.semantics.*
import dev.morphia.audits.shell.*
import org.bson.*
import org.bson.types.Decimal128
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PublishedExpectationsTest {
    private fun pattern(value: PublishedValue) =
        PublishedDocument(listOf(PublishedField("v", value)))

    private fun document(value: BsonValue) = BsonDocument("v", value)

    private fun exact(value: BsonValue) = PublishedExact(BsonSnapshot.of(value))

    private fun compare(value: PublishedValue, actual: BsonValue) =
        PublishedComparison.compareResults(listOf(pattern(value)), listOf(document(actual)))

    private fun request(
        output: String,
        action: String = "db.items.find({});",
        dialect: ShellDialect = ShellDialect.MONGOSH,
    ) =
        ShellProgramRequest(
            dialect = dialect,
            blocks =
                listOf(
                    ProgramBlock("action", action, ProgramRole.ACTION),
                    ProgramBlock("result", output, ProgramRole.EXPECTED),
                ),
        )

    private fun parsed(output: String): PublishedDocument {
        val result = ShellPrograms.normalize(request(output))
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        return result.expectations.single().published!!.single()
    }

    @Test
    fun wholeNumberIsAClosedAlternativeRatherThanNumericTolerance() {
        val expected = PublishedWholeNumber(2)
        for (v in listOf(BsonInt32(2), BsonDouble(2.0))) assertEquals(
            BsonComparisonState.EQUAL,
            compare(expected, v).state,
        )
        for (v in
            listOf(
                BsonInt64(2),
                BsonDecimal128(Decimal128.parse("2")),
                BsonDouble(2.01),
                BsonInt32(3),
            )) assertEquals(BsonComparisonState.DIFFERENT, compare(expected, v).state)
        assertEquals(
            BsonComparisonState.DIFFERENT,
            BsonComparison.compare(BsonInt32(2), BsonDouble(2.0)).state,
        )
    }

    @Test
    fun negativeZeroNeverMatchesPrintedPositiveZero() {
        assertEquals(
            BsonComparisonState.DIFFERENT,
            compare(PublishedWholeNumber(0), BsonDouble(-0.0)).state,
        )
        val v = parsed("[{v:-0}]").fields.single().value
        assertEquals(BsonComparisonState.EQUAL, compare(v, BsonDouble(-0.0)).state)
        assertEquals(BsonComparisonState.DIFFERENT, compare(v, BsonInt32(0)).state)
        assertEquals(BsonComparisonState.DIFFERENT, compare(v, BsonDouble(0.0)).state)
    }

    @Test
    fun explicitConstructorsRetainTypesAndFractionsRemainDouble() {
        for ((source, good, bad) in
            listOf(
                Triple("NumberInt(2)", BsonInt32(2), BsonDouble(2.0)),
                Triple("Double(2)", BsonDouble(2.0), BsonInt32(2)),
                Triple("2.5", BsonDouble(2.5), BsonDecimal128(Decimal128.parse("2.5"))),
                Triple("NumberLong('2')", BsonInt64(2), BsonDouble(2.0)),
            )) {
            val v = parsed("[{v:$source}]").fields.single().value
            assertEquals(BsonComparisonState.EQUAL, compare(v, good).state)
            assertEquals(BsonComparisonState.DIFFERENT, compare(v, bad).state)
        }
    }

    @Test
    fun textAccountsForLegacySymbolPromotion() {
        val v = parsed("[{v:'text'}]").fields.single().value
        assertEquals(PublishedText("text"), v)
        assertEquals(BsonComparisonState.EQUAL, compare(v, BsonString("text")).state)
        assertEquals(BsonComparisonState.EQUAL, compare(v, BsonSymbol("text")).state)
        assertEquals(BsonComparisonState.DIFFERENT, compare(v, BsonString("other")).state)
        assertEquals(
            BsonComparisonState.DIFFERENT,
            BsonComparison.compare(BsonString("text"), BsonSymbol("text")).state,
        )
    }

    @Test
    fun nanDoesNotInventUnprintedPayloadBits() {
        val v = parsed("[{v:NaN}]").fields.single().value
        val a = BsonDouble(Double.fromBits(0x7ff8000000000001L))
        val b = BsonDouble(Double.fromBits(0x7ff8000000000002L))
        assertEquals(PublishedDoubleNaN, v)
        assertEquals(BsonComparisonState.EQUAL, compare(v, a).state)
        assertEquals(BsonComparisonState.EQUAL, compare(v, b).state)
        assertEquals(BsonComparisonState.DIFFERENT, BsonComparison.compare(a, b).state)
        assertEquals(
            BsonComparisonState.DIFFERENT,
            compare(v, BsonDouble(Double.POSITIVE_INFINITY)).state,
        )
    }

    @Test
    fun decimalTextPreservesPrintedQuantumAndSignedZero() {
        for (source in listOf("1.20", "-0", "NaN", "Infinity")) {
            val v = parsed("[{v:NumberDecimal('$source')}]").fields.single().value
            assertEquals(PublishedDecimal128(source), v)
            assertEquals(
                BsonComparisonState.EQUAL,
                compare(v, BsonDecimal128(Decimal128.parse(source))).state,
            )
        }
        assertEquals(
            BsonComparisonState.DIFFERENT,
            compare(PublishedDecimal128("1.20"), BsonDecimal128(Decimal128.parse("1.2"))).state,
        )
        assertThrows(IllegalArgumentException::class.java) { PublishedDecimal128("01.20") }
    }

    @Test
    fun javascriptRegexNeedsAnExplicitUnambiguousDisposition() {
        for (value in listOf("/a/i", "RegExp('a','i')", "new RegExp('a','i')")) {
            val result = ShellPrograms.normalize(request("[{v:$value}]"))
            assertEquals("PUBLISHED_REGEX", result.problems.single().code)
            assertTrue(result.expectations.isEmpty())
        }
        assertEquals(
            BsonComparisonState.EQUAL,
            compare(
                    parsed("[{v:BSONRegExp('a','i')}]").fields.single().value,
                    BsonRegularExpression("a", "i"),
                )
                .state,
        )
    }

    @Test
    fun nestedArraysAndPresenceRemainStrict() {
        val v = PublishedArray(listOf(PublishedWholeNumber(1), PublishedWholeNumber(2)))
        assertEquals(
            BsonComparisonState.DIFFERENT,
            compare(v, BsonArray(listOf(BsonInt32(2), BsonInt32(1)))).state,
        )
        assertEquals(
            BsonComparisonState.DIFFERENT,
            compare(v, BsonArray(listOf(BsonInt32(1)))).state,
        )
        val p = listOf(pattern(PublishedWholeNumber(1)))
        assertEquals(
            BsonDifferenceKind.MISSING_FIELD,
            PublishedComparison.compareResults(p, listOf(BsonDocument())).difference!!.kind,
        )
        assertEquals(
            BsonDifferenceKind.EXTRA_FIELD,
            PublishedComparison.compareResults(
                    p,
                    listOf(document(BsonInt32(1)).append("extra", BsonNull.VALUE)),
                )
                .difference!!
                .kind,
        )
    }

    @Test
    fun unorderedRowsUseBipartiteMatchingAndPreserveMultiplicity() {
        val expected = listOf(pattern(PublishedWholeNumber(2)), pattern(exact(BsonDouble(2.0))))
        val actual = listOf(document(BsonDouble(2.0)), document(BsonInt32(2)))
        assertEquals(
            BsonComparisonState.EQUAL,
            PublishedComparison.compareResults(expected, actual, ResultOrder.UNORDERED).state,
        )
        assertEquals(
            BsonComparisonState.DIFFERENT,
            PublishedComparison.compareResults(expected, actual).state,
        )
        assertEquals(
            BsonComparisonState.DIFFERENT,
            PublishedComparison.compareResults(
                    expected,
                    listOf(document(BsonInt32(2)), document(BsonInt32(2))),
                    ResultOrder.UNORDERED,
                )
                .state,
        )
        assertEquals(
            BsonDifferenceKind.RESULT_COUNT,
            PublishedComparison.compareResults(expected, actual.take(1)).difference!!.kind,
        )
    }

    @Test
    fun numericKeysUseJavascriptEnumerationButTypedParityKeepsRawBsonOrder() {
        val p = parsed("[{'2':2,'1':1,z:3,'01':4,'4294967295':5,'4294967294':6}]")
        assertEquals(
            listOf("1", "2", "4294967294", "z", "01", "4294967295"),
            p.fields.map { it.name },
        )
        val actual = BsonDocument.parse("{'2':2,'1':1,'z':3,'01':4,'4294967295':5,'4294967294':6}")
        assertEquals(
            BsonComparisonState.EQUAL,
            PublishedComparison.compareResults(listOf(p), listOf(actual)).state,
        )
        val canonical =
            BsonDocument().apply {
                JavaScriptKeyOrder.order(actual.keys.toList()).forEach { this[it] = actual[it] }
            }
        assertEquals(BsonComparisonState.DIFFERENT, BsonComparison.compare(actual, canonical).state)
        val lower = ShellLiterals.normalize("{'2':2,'1':1,z:3}", ShellDialect.MONGOSH)
        assertEquals(listOf("1", "2", "z"), lower.value!!.asDocument().keys.toList())
    }

    @Test
    fun ordinaryFieldOrderIsStillObservable() {
        val p = parsed("[{a:1,b:2}]")
        assertEquals(
            BsonDifferenceKind.KEY_ORDER,
            PublishedComparison.compareResults(
                    listOf(p),
                    listOf(BsonDocument("b", BsonInt32(2)).append("a", BsonInt32(1))),
                )
                .difference!!
                .kind,
        )
    }

    @Test
    fun bindingsKeepCapturedValuesAndDependencies() {
        val result =
            ShellPrograms.normalize(
                request("[{v:x}]", "let n=2; const x=n; n=3; db.items.find({v:x});")
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        val expectation = result.expectations.single()
        assertEquals(
            PublishedWholeNumber(2),
            expectation.published!!.single().fields.single().value,
        )
        assertEquals(2, expectation.dependencies.size)
        assertNotEquals(
            ShellPrograms.normalize(request("[{v:3}]", "db.items.find({v:2});")).semanticSha256,
            result.semanticSha256,
        )
    }

    @Test
    fun publishedProfileRequiresModernReadResults() {
        for (dialect in listOf(ShellDialect.LEGACY_MONGO, ShellDialect.UNSPECIFIED)) assertEquals(
            "PUBLISHED_PROFILE",
            ShellPrograms.normalize(request("[{v:NumberInt(2)}]", dialect = dialect))
                .problems
                .single()
                .code,
        )
        assertEquals(
            "PUBLISHED_PROFILE",
            ShellPrograms.normalize(request("[{v:2}]", "db.items.insertOne({v:2});"))
                .problems
                .single()
                .code,
        )
        for (option in
            listOf(
                "promoteLongs",
                "promoteValues",
                "promoteBuffers",
                "useBigInt64",
                "bsonRegExp",
                "raw",
                "fieldsAsRaw",
                "enableUtf8Validation",
            )) assertEquals(
            "PUBLISHED_PROFILE",
            ShellPrograms.normalize(request("[{v:2}]", "db.items.aggregate([],{$option:false});"))
                .problems
                .single()
                .code,
        )
        assertEquals(
            ShellLiteralState.NORMALIZED,
            ShellPrograms.normalize(request("[{v:2}]", "db.items.find({}, {promoteLongs:1});"))
                .state,
        )
    }

    @Test
    fun legacyDocumentFormatRetainsExplicitTypedAssertions() {
        val req = request("{v:2}", dialect = ShellDialect.LEGACY_MONGO)
        val result =
            ShellPrograms.normalize(
                req.copy(
                    blocks =
                        listOf(
                            req.blocks[0],
                            req.blocks[1].copy(format = ProgramFormat.LEGACY_DOCUMENTS),
                        )
                )
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state)
        assertNotNull(result.expectations.single().documents)
        assertNull(result.expectations.single().published)
    }

    @Test
    fun outputIsAnExplicitVersionedConstraintArtifact() {
        val result = ShellPrograms.normalize(request("[{v:2,s:'text'}]"))
        assertEquals(2, result.schemaVersion)
        val json = AuditJson.write(result)
        assertTrue(json.contains("WHOLE_NUMBER"))
        assertTrue(json.contains("ECMASCRIPT_OWN_PROPERTIES"))
        assertNull(result.expectations.single().documents)
        assertEquals(json, AuditJson.write(ShellPrograms.normalize(request("[{v:2,s:'text'}]"))))
    }

    @Test
    fun malformedTreesAreInvalidEvenBehindAnEarlyMismatch() {
        @Suppress("UNCHECKED_CAST")
        val fields = listOf<PublishedField?>(null) as List<PublishedField>
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(listOf(PublishedDocument(fields)), emptyList()).state,
        )
        val cycle = BsonDocument("v", BsonInt32(99))
        cycle["cycle"] = cycle
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(
                    listOf(pattern(PublishedWholeNumber(2))),
                    listOf(cycle),
                )
                .state,
        )
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(emptyList(), listOf(cycle)).state,
        )
        val items = mutableListOf<PublishedValue>()
        val loop = PublishedArray(items)
        items += loop
        assertEquals(BsonComparisonState.INVALID, compare(loop, BsonInt32(0)).state)
        assertEquals(
            BsonComparisonState.INVALID,
            compare(
                    PublishedDocument(
                        listOf(
                            PublishedField("x", PublishedWholeNumber(1)),
                            PublishedField("x", PublishedWholeNumber(2)),
                        )
                    ),
                    BsonNull.VALUE,
                )
                .state,
        )
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(
                    listOf(pattern(PublishedWholeNumber(2))),
                    listOf(RawBsonDocument.parse("{v:2}")),
                )
                .state,
        )
    }

    @Test
    fun stringAndKeyEncodingAreValidatedBeforeMatching() {
        for (value in listOf("\uD800", "\uDC00")) {
            assertEquals(
                BsonComparisonState.INVALID,
                compare(PublishedText(value), BsonString("other")).state,
            )
            assertEquals(
                BsonComparisonState.INVALID,
                compare(PublishedWholeNumber(1), BsonString(value)).state,
            )
        }
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(
                    emptyList(),
                    listOf(BsonDocument("bad\u0000key", BsonInt32(2))),
                )
                .state,
        )
    }

    @Test
    fun comparisonLimitsAreSharedAcrossAllRowsAndMatching() {
        val p = listOf(pattern(PublishedWholeNumber(2)), pattern(PublishedWholeNumber(2)))
        val a = listOf(document(BsonInt32(2)), document(BsonInt32(2)))
        for (limits in
            listOf(
                BsonComparisonLimits(maximumResults = 1),
                BsonComparisonLimits(maximumWork = 2),
                BsonComparisonLimits(maximumDepth = 1, maximumResultPairs = 1),
            )) {
            assertEquals(
                BsonComparisonState.INVALID,
                PublishedComparison.compareResults(p, a, ResultOrder.UNORDERED, limits).state,
            )
        }
        val deep = PublishedArray(listOf(PublishedArray(listOf(PublishedWholeNumber(2)))))
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(
                    listOf(pattern(deep)),
                    listOf(document(BsonInt32(0))),
                    limits = BsonComparisonLimits(maximumDepth = 1),
                )
                .state,
        )
    }

    @Test
    fun artifactAndExpandedTextLimitsPreventSmallBindingAmplification() {
        val expanded =
            ShellPrograms.normalize(
                request(
                    "[{v:[" + List(50) { "x" }.joinToString(",") + "]}]",
                    "const x='" + "x".repeat(500) + "'; db.items.find({});",
                ),
                ShellProgramLimits(maximumArtifactBytes = 8192),
            )
        assertEquals("ARTIFACT_LIMIT", expanded.problems.single().code)
        assertTrue(expanded.commands.isEmpty())
        assertTrue(expanded.expectations.isEmpty())
        val code = "const x='" + "x".repeat(100_000) + "'; db.items.find({});"
        val output = "[{v:[" + List(180) { "x" }.joinToString(",") + "]}]"
        val result =
            ShellPrograms.normalize(
                request(output, code),
                ShellProgramLimits(maximumStoredValueBytes = 67_108_864),
            )
        assertEquals("VALUE_LIMIT", result.problems.single().code)
        assertTrue(result.expectations.isEmpty())
    }

    @Test
    fun decodingOverridesAndWriteResultsCannotBorrowReadProfile() {
        for (option in
            listOf(
                "promoteLongs",
                "promoteValues",
                "promoteBuffers",
                "useBigInt64",
                "bsonRegExp",
                "raw",
                "fieldsAsRaw",
                "enableUtf8Validation",
            )) {
            val result =
                ShellPrograms.normalize(
                    request("[{v:2}]", "db.items.aggregate([],{" + option + ":false});")
                )
            assertEquals("PUBLISHED_PROFILE", result.problems.single().code, option)
            assertTrue(result.commands.isEmpty())
            val projection =
                ShellPrograms.normalize(request("[{v:2}]", "db.items.find({},{" + option + ":1});"))
            assertEquals(ShellLiteralState.NORMALIZED, projection.state, option)
        }
        for (command in
            listOf(
                "db.items.insertOne({v:2});",
                "db.items.updateOne({},{\$set:{v:2}});",
                "db.items.createIndex({v:1});",
            )) {
            assertEquals(
                "PUBLISHED_PROFILE",
                ShellPrograms.normalize(request("[{v:2}]", command)).problems.single().code,
            )
        }
    }

    @Test
    fun legacyDocumentStreamsKeepIndependentTypedExpectations() {
        val req = request("{v:2}", dialect = ShellDialect.LEGACY_MONGO)
        val result =
            ShellPrograms.normalize(
                req.copy(
                    blocks =
                        listOf(
                            req.blocks[0],
                            req.blocks[1].copy(format = ProgramFormat.LEGACY_DOCUMENTS),
                        )
                )
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        val expectation = result.expectations.single()
        assertNull(expectation.published)
        assertNull(expectation.publishedProfile)
        assertEquals(
            BsonType.INT32,
            expectation.documents!!.single().value().asDocument().getValue("v").bsonType,
        )
    }

    @Test
    fun artifactsRecordProfileAndDoNotHydratePatterns() {
        val result = ShellPrograms.normalize(request("[{v:2}]"))
        val json = AuditJson.write(result)
        assertEquals(2, result.schemaVersion)
        assertEquals(PublishedReadProfile.ID, result.expectations.single().publishedProfile)
        assertTrue(json.contains("WHOLE_NUMBER"))
        assertTrue(json.contains(PublishedReadProfile.ID))
        assertThrows(IllegalArgumentException::class.java) { AuditJson.programRequest(json) }
        val typed = ShellPrograms.normalize(request("[{v:NumberInt(2)}]"))
        assertNotEquals(result.semanticSha256, typed.semanticSha256)
        assertEquals(
            result.semanticSha256,
            ShellPrograms.normalize(request(" [ { v: 2 } ] ")).semanticSha256,
        )
    }

    @Test
    fun hiddenNullsScopeCyclesAndOversizedValuesAreInvalid() {
        val nullArray = BsonArray()
        nullArray.add(null)
        val document = BsonDocument("hidden", nullArray)
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(emptyList(), listOf(document)).state,
        )
        val scope = BsonDocument()
        scope["cycle"] = BsonJavaScriptWithScope("x", scope)
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(emptyList(), listOf(scope)).state,
        )
        val result =
            PublishedComparison.compareResults(
                listOf(pattern(PublishedWholeNumber(2))),
                listOf(document(BsonString("x".repeat(200)))),
                limits = BsonComparisonLimits(maximumWork = 100),
            )
        assertEquals(BsonComparisonState.INVALID, result.state)
    }

    @Test
    fun publishedScalarsUseExactTypedComparisonWhereDisplayIsExplicit() {
        for ((source, value) in
            listOf(
                "true" to BsonBoolean.TRUE,
                "null" to BsonNull.VALUE,
                "ObjectId('507f1f77bcf86cd799439011')" to
                    BsonObjectId(org.bson.types.ObjectId("507f1f77bcf86cd799439011")),
                "ISODate('2024-01-01T00:00:00Z')" to BsonDateTime(1704067200000),
                "BinData(0,'AQI=')" to BsonBinary(byteArrayOf(1, 2)),
            )) {
            assertEquals(
                BsonComparisonState.EQUAL,
                compare(parsed("[{v:$source}]").fields.single().value, value).state,
                source,
            )
        }
    }

    @Test
    fun exactScalarTypeMismatchDoesNotRevalidateDeepContainersWithDefaultLimits() {
        var actual: BsonValue = BsonInt32(1)
        repeat(150) { actual = BsonArray(listOf(actual)) }
        val result =
            PublishedComparison.compareResults(
                listOf(pattern(exact(BsonInt64(1)))),
                listOf(document(actual)),
                limits = BsonComparisonLimits(maximumDepth = 200),
            )
        assertEquals(BsonComparisonState.DIFFERENT, result.state, result.problem)
        assertEquals(BsonDifferenceKind.TYPE, result.difference!!.kind)
    }

    @Test
    fun longFieldNamesSpendWorkForEachCandidatePair() {
        val key = "x".repeat(1000)
        val expected =
            List(4) { PublishedDocument(listOf(PublishedField(key, PublishedWholeNumber(1)))) }
        val actual = List(4) { BsonDocument(key, BsonInt32(1)) }
        val result =
            PublishedComparison.compareResults(
                expected,
                actual,
                ResultOrder.UNORDERED,
                BsonComparisonLimits(maximumWork = 12_000),
            )
        assertEquals(BsonComparisonState.INVALID, result.state)
    }

    @Test
    fun dbRefShapesAreConservativelyUnsupportedAtEveryDepth() {
        for (output in
            listOf(
                "[{\$ref:'items',\$id:1}]",
                "[{v:{\$ref:'items',\$id:ObjectId('507f1f77bcf86cd799439011')}}]",
                "[{v:[{\$ref:'items',\$id:1,\$db:'db'}]}]",
            )) {
            val result = ShellPrograms.normalize(request(output))
            assertEquals("PUBLISHED_DBREF", result.problems.single().code)
            assertTrue(result.expectations.isEmpty())
        }
        val manual =
            PublishedDocument(
                listOf(
                    PublishedField("\$ref", PublishedText("items")),
                    PublishedField("\$id", PublishedWholeNumber(1)),
                )
            )
        assertEquals(
            BsonComparisonState.INVALID,
            PublishedComparison.compareResults(listOf(manual), emptyList()).state,
        )
        // A single similarly named field is ordinary data, and input storage remains unchanged.
        parsed("[{\$ref:'items'}]")
        assertEquals(
            ShellLiteralState.NORMALIZED,
            ShellLiterals.normalize("{\$ref:'items',\$id:1}", ShellDialect.MONGOSH).state,
        )
    }

    @Test
    fun typedSnapshotsAndPublishedTextShareTheStoredValueBudget() {
        val limits = ShellProgramLimits(maximumSnapshotBytes = 1024, maximumStoredValueBytes = 4096)
        val req = request("[{v:'" + "y".repeat(3000) + "'}]")
        assertEquals(ShellLiteralState.NORMALIZED, ShellPrograms.normalize(req, limits).state)
        val seeded =
            req.copy(
                blocks =
                    listOf(
                        ProgramBlock(
                            "seed",
                            "db.items.insertOne({v:'" + "x".repeat(900) + "'});",
                            ProgramRole.SETUP,
                        )
                    ) + req.blocks
            )
        val result = ShellPrograms.normalize(seeded, limits)
        assertEquals("STORED_VALUE_LIMIT", result.problems.single().code)
        assertTrue(result.commands.isEmpty())
    }
}
