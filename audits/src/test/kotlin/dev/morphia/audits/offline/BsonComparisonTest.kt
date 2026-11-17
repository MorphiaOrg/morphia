package dev.morphia.audits.offline

import dev.morphia.audits.semantics.*
import java.math.BigDecimal
import org.bson.*
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class BsonComparisonTest {
    private fun doc(json: String) = BsonDocument.parse(json)

    private fun equal(
        a: BsonValue,
        b: BsonValue,
        policy: BsonComparisonPolicy = BsonComparisonPolicy(),
    ) {
        val result = BsonComparison.compare(a, b, policy)
        assertEquals(BsonComparisonState.EQUAL, result.state, result.toString())
    }

    private fun different(
        a: BsonValue,
        b: BsonValue,
        kind: BsonDifferenceKind = BsonDifferenceKind.VALUE,
        policy: BsonComparisonPolicy = BsonComparisonPolicy(),
    ): BsonComparisonResult {
        val result = BsonComparison.compare(a, b, policy)
        assertEquals(BsonComparisonState.DIFFERENT, result.state, result.toString())
        assertEquals(kind, result.difference!!.kind)
        return result
    }

    private val ignoreRootOrder = BsonComparisonPolicy(unorderedDocuments = setOf(BsonPath.ROOT))

    private fun tolerance(path: String, absolute: String, relative: String = "0") =
        BsonComparisonPolicy(
            numericTolerances =
                mapOf(
                    BsonPath(path) to NumericTolerance(BigDecimal(absolute), BigDecimal(relative))
                )
        )

    @Test
    fun numericTypesNeverCollapse() {
        val values =
            listOf<BsonValue>(
                BsonInt32(1),
                BsonInt64(1),
                BsonDouble(1.0),
                BsonDecimal128(Decimal128.parse("1")),
            )
        values.forEach { left ->
            values.forEach { right ->
                if (left.bsonType == right.bsonType) equal(left, right)
                else different(left, right, BsonDifferenceKind.TYPE, tolerance("", "100"))
            }
        }
    }

    @Test
    fun scalarBsonValuesRetainTheirTypesAndContents() {
        val values =
            listOf<BsonValue>(
                BsonBoolean.TRUE,
                BsonBoolean.FALSE,
                BsonString("https://host/a//b: c"),
                BsonNull.VALUE,
                BsonUndefined(),
                BsonMinKey(),
                BsonMaxKey(),
                BsonDateTime(1234),
                BsonTimestamp(3, 4),
                BsonObjectId(ObjectId("507f1f77bcf86cd799439011")),
                BsonBinary(0x00.toByte(), byteArrayOf(0, 1, -1)),
                BsonRegularExpression("a+", "im"),
                BsonJavaScript("return 1;"),
                BsonSymbol("x"),
                BsonDbPointer("db.items", ObjectId("507f1f77bcf86cd799439011")),
                BsonInt32(3),
                BsonInt64(4),
                BsonDouble(5.0),
                BsonDecimal128(Decimal128.parse("6.00")),
            )
        values.forEach { left ->
            values.forEach { right ->
                if (left === right) equal(left, right)
                else
                    assertEquals(
                        BsonComparisonState.DIFFERENT,
                        BsonComparison.compare(left, right).state,
                    )
            }
        }
        different(BsonRegularExpression("a", "i"), BsonRegularExpression("a", "m"))
        different(BsonBinary(0.toByte(), byteArrayOf(1)), BsonBinary(1.toByte(), byteArrayOf(1)))
        different(BsonDateTime(1), BsonDateTime(2))
        different(BsonTimestamp(1, 2), BsonTimestamp(2, 1))
        different(
            BsonDbPointer("a", ObjectId("507f1f77bcf86cd799439011")),
            BsonDbPointer("b", ObjectId("507f1f77bcf86cd799439011")),
        )
    }

    @Test
    fun missingAndNullAreDistinctAndExtraFieldsAlwaysFail() {
        val absent = different(doc("{a:null}"), doc("{}"), BsonDifferenceKind.MISSING_FIELD)
        assertEquals("/a", absent.difference!!.path.pointer)
        different(doc("{}"), doc("{a:null}"), BsonDifferenceKind.EXTRA_FIELD, ignoreRootOrder)
        different(BsonNull.VALUE, BsonUndefined(), BsonDifferenceKind.TYPE)
    }

    @Test
    fun documentOrderIsStrictUnlessTheExactDocumentIsNamed() {
        val a = doc("{x:{a:1,b:2},y:3}")
        val b = doc("{y:3,x:{b:2,a:1}}")
        different(a, b, BsonDifferenceKind.KEY_ORDER)
        val nested = different(a, b, BsonDifferenceKind.KEY_ORDER, ignoreRootOrder)
        assertEquals("/x", nested.difference!!.path.pointer)
        equal(a, b, BsonComparisonPolicy(unorderedDocuments = setOf(BsonPath.ROOT, BsonPath("/x"))))
    }

    @Test
    fun sortCompoundIndexAndEmbeddedDocumentOrderRemainMeaningful() {
        for (key in listOf("\$sort", "indexKeys", "embeddedEquality")) {
            val a = BsonDocument(key, doc("{a:1,b:-1}"))
            val b = BsonDocument(key, doc("{b:-1,a:1}"))
            different(a, b, BsonDifferenceKind.KEY_ORDER, ignoreRootOrder)
        }
    }

    @Test
    fun pipelinesAndNestedArraysAreNeverReordered() {
        different(
            BsonArray(listOf(doc("{\$match:{a:1}}"), doc("{\$limit:1}"))),
            BsonArray(listOf(doc("{\$limit:1}"), doc("{\$match:{a:1}}"))),
            BsonDifferenceKind.MISSING_FIELD,
        )
        different(doc("{a:[1,2]}"), doc("{a:[2,1]}"))
        different(doc("{a:[1]}"), doc("{a:[1,1]}"), BsonDifferenceKind.ARRAY_LENGTH)
    }

    @Test
    fun pathsEscapeKeysAndAddressArrayItemsPrecisely() {
        val mismatch = different(doc("{'a/b':{'~': [0,1]}}"), doc("{'a/b':{'~':[0,2]}}"))
        assertEquals("/a~1b/~0/1", mismatch.difference!!.path.pointer)
        equal(doc("{'a/b':[1.0]}"), doc("{'a/b':[1.01]}"), tolerance("/a~1b/0", "0.02"))
        listOf("a", "/~", "/~2", "/a~b").forEach { path ->
            assertThrows(IllegalArgumentException::class.java) { BsonPath(path) }
        }
        assertEquals("", BsonPath.ROOT.pointer)
        assertEquals("/", BsonPath("/").pointer)
    }

    @Test
    fun finiteToleranceIsPathSpecificAndTypeSpecific() {
        equal(BsonDouble(1.0), BsonDouble(1.01), tolerance("", "0.02"))
        different(BsonDouble(1.0), BsonDouble(1.01))
        different(doc("{a:1.0,b:1.0}"), doc("{a:1.01,b:1.01}"), policy = tolerance("/a", "0.02"))
        equal(BsonInt64(Long.MAX_VALUE), BsonInt64(Long.MAX_VALUE - 1), tolerance("", "1"))
        different(
            BsonInt64(Long.MAX_VALUE),
            BsonInt64(Long.MAX_VALUE - 2),
            policy = tolerance("", "1"),
        )
        equal(BsonDouble(100.0), BsonDouble(101.0), tolerance("", "0", "0.01"))
        different(BsonDouble(100.0), BsonDouble(102.0), policy = tolerance("", "0", "0.01"))
        different(BsonInt32(1), BsonInt64(1), BsonDifferenceKind.TYPE, tolerance("", "100"))
        assertThrows(IllegalArgumentException::class.java) { NumericTolerance(BigDecimal("-1")) }
        assertThrows(IllegalArgumentException::class.java) {
            NumericTolerance(relative = BigDecimal("-1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NumericTolerance(BigDecimal("1e999999"))
        }
    }

    @Test
    fun decimalAndDoubleRepresentationsArePreservedUnlessExplicitlyRelaxed() {
        val a = BsonDecimal128(Decimal128.parse("1.0"))
        val b = BsonDecimal128(Decimal128.parse("1.00"))
        different(a, b)
        equal(a, b, tolerance("", "0"))
        different(BsonDouble(-0.0), BsonDouble(0.0))
        equal(BsonDouble(-0.0), BsonDouble(0.0), tolerance("", "0"))
        val minusZero = BsonDecimal128(Decimal128.NEGATIVE_ZERO)
        val plusZero = BsonDecimal128(Decimal128.POSITIVE_ZERO)
        different(minusZero, plusZero)
        equal(minusZero, plusZero, tolerance("", "0"))
        val nan = BsonDouble(Double.fromBits(0x7ff8000000000001L))
        equal(nan, BsonDouble(Double.fromBits(0x7ff8000000000001L)))
        different(
            nan,
            BsonDouble(Double.fromBits(0x7ff8000000000002L)),
            policy = tolerance("", "100"),
        )
    }

    @Test
    fun specialNumericValuesNeverBecomeFiniteToleranceMatches() {
        for (value in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            equal(BsonDouble(value), BsonDouble(value))
            different(BsonDouble(value), BsonDouble(1.0), policy = tolerance("", "1e100"))
        }
        for (value in
            listOf(
                Decimal128.NaN,
                Decimal128.NEGATIVE_NaN,
                Decimal128.POSITIVE_INFINITY,
                Decimal128.NEGATIVE_INFINITY,
            )) {
            equal(BsonDecimal128(value), BsonDecimal128(value))
            different(
                BsonDecimal128(value),
                BsonDecimal128(Decimal128.parse("1")),
                policy = tolerance("", "1e100"),
            )
        }
    }

    @Test
    fun javascriptScopeUsesTreeComparisonWithoutExecutingCode() {
        val a = BsonJavaScriptWithScope("throw new Error('never run')", doc("{a:1,b:2}"))
        equal(a, BsonJavaScriptWithScope(a.code, doc("{a:1,b:2}")))
        different(a, BsonJavaScriptWithScope("return 1", a.scope))
        val b = BsonJavaScriptWithScope(a.code, doc("{b:2,a:1}"))
        different(a, b, BsonDifferenceKind.KEY_ORDER)
        equal(a, b, BsonComparisonPolicy(unorderedDocuments = setOf(BsonPath("/\$scope"))))
    }

    @Test
    fun orderedResultsPreserveRowPosition() {
        val a = listOf(doc("{v:1}"), doc("{v:2}"))
        val result = BsonComparison.compareResults(a, a.reversed())
        assertEquals(BsonComparisonState.DIFFERENT, result.state)
        assertEquals("/0/v", result.difference!!.path.pointer)
        assertEquals(BsonComparisonState.EQUAL, BsonComparison.compareResults(a, a).state)
    }

    @Test
    fun unorderedResultsPreserveMultiplicityAndNestedArrayOrder() {
        val a = listOf(doc("{v:1}"), doc("{v:1}"), doc("{v:2}"))
        assertEquals(
            BsonComparisonState.EQUAL,
            BsonComparison.compareResults(a, a.reversed(), ResultOrder.UNORDERED).state,
        )
        val lost =
            BsonComparison.compareResults(
                a,
                listOf(doc("{v:1}"), doc("{v:2}"), doc("{v:2}")),
                ResultOrder.UNORDERED,
            )
        assertEquals(BsonComparisonState.DIFFERENT, lost.state)
        assertEquals(BsonDifferenceKind.UNMATCHED_RESULT, lost.difference!!.kind)
        assertEquals(
            BsonComparisonState.DIFFERENT,
            BsonComparison.compareResults(
                    listOf(doc("{v:[1,2]}")),
                    listOf(doc("{v:[2,1]}")),
                    ResultOrder.UNORDERED,
                )
                .state,
        )
        assertEquals(
            BsonDifferenceKind.RESULT_COUNT,
            BsonComparison.compareResults(a, a.take(1), ResultOrder.UNORDERED).difference!!.kind,
        )
    }

    @Test
    fun multisetMatchingFindsAssignmentsThatGreedyMatchingMisses() {
        // Expected 1 can take 0 or 2, but expected 0 can take only 0.
        val policy = tolerance("/v", "1")
        val a = listOf(doc("{v:1}"), doc("{v:0}"))
        val b = listOf(doc("{v:0}"), doc("{v:2}"))
        assertEquals(
            BsonComparisonState.EQUAL,
            BsonComparison.compareResults(a, b, ResultOrder.UNORDERED, policy).state,
        )
        assertEquals(
            BsonComparisonState.EQUAL,
            BsonComparison.compareResults(b, a, ResultOrder.UNORDERED, policy).state,
        )
    }

    @Test
    fun resultPoliciesAreRelativeToEveryRowRatherThanItsPosition() {
        val a = listOf(doc("{id:1,v:1.0}"), doc("{id:2,v:2.0}"))
        val b = listOf(doc("{v:2.01,id:2}"), doc("{v:1.01,id:1}"))
        val policy = tolerance("/v", "0.02").copy(unorderedDocuments = setOf(BsonPath.ROOT))
        assertEquals(
            BsonComparisonState.EQUAL,
            BsonComparison.compareResults(a, b, ResultOrder.UNORDERED, policy).state,
        )
        assertEquals(
            BsonComparisonState.DIFFERENT,
            BsonComparison.compareResults(a, b, ResultOrder.ORDERED, policy).state,
        )
    }

    @Test
    fun cyclesAndResourceLimitsAreInvalidEvenBehindEarlyMismatches() {
        val cycle = BsonDocument()
        cycle["self"] = cycle
        assertEquals(BsonComparisonState.INVALID, BsonComparison.compare(BsonInt32(1), cycle).state)
        val array = BsonArray()
        array.add(array)
        assertEquals(BsonComparisonState.INVALID, BsonComparison.compare(array, array).state)
        val deep = BsonArray(listOf(BsonArray(listOf(BsonInt32(1)))))
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compare(deep, deep, limits = BsonComparisonLimits(maximumDepth = 1))
                .state,
        )
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compare(
                    doc("{a:1}"),
                    doc("{a:1}"),
                    limits = BsonComparisonLimits(maximumWork = 1),
                )
                .state,
        )
        val rows = listOf(doc("{a:1}"), doc("{a:2}"))
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compareResults(
                    rows,
                    rows,
                    limits = BsonComparisonLimits(maximumResults = 1),
                )
                .state,
        )
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compareResults(
                    rows,
                    rows,
                    ResultOrder.UNORDERED,
                    limits = BsonComparisonLimits(maximumResultPairs = 3),
                )
                .state,
        )
    }

    @Test
    fun sharedAcyclicSubtreesAndEmptyValuesAreValid() {
        val child = doc("{x:1}")
        equal(BsonArray(listOf(child, child)), BsonArray(listOf(doc("{x:1}"), doc("{x:1}"))))
        equal(BsonDocument(), BsonDocument())
        equal(BsonArray(), BsonArray())
        for (order in ResultOrder.entries) assertEquals(
            BsonComparisonState.EQUAL,
            BsonComparison.compareResults(emptyList(), emptyList(), order).state,
        )
    }

    @Test
    fun legacyStrictComparisonAcceptsNumericAndKeyOrderDrift() {
        // Mirrors TestBase's JSONAssert(..., strict=true) over relaxed driver JSON.
        for ((a, b) in
            listOf(
                doc("{v:1}") to doc("{v:1.0}"),
                doc("{v:1}") to doc("{v:{\$numberLong:'1'}}"),
                doc("{\$sort:{a:1,b:-1}}") to doc("{\$sort:{b:-1,a:1}}"),
            )) {
            JSONAssert.assertEquals(a.toJson(), b.toJson(), true)
            assertEquals(BsonComparisonState.DIFFERENT, BsonComparison.compare(a, b).state)
        }
    }

    @Test
    fun legacyLenientComparisonAcceptsExtraFieldsAndReorderedNestedArrays() {
        for ((a, b) in
            listOf(
                doc("{v:1}") to doc("{v:1,extra:2}"),
                doc("{v:[1,2]}") to doc("{v:[2,1]}"),
                doc("{pipeline:[{\$match:{v:1}},{\$limit:1}]}") to
                    doc("{pipeline:[{\$limit:1},{\$match:{v:1}}]}"),
            )) {
            JSONAssert.assertEquals(a.toJson(), b.toJson(), false)
            assertEquals(BsonComparisonState.DIFFERENT, BsonComparison.compare(a, b).state)
        }
    }

    @Test
    fun rawDuplicateCorruptAndLazyDocumentsRequireValidatedLowering() {
        val duplicate =
            RawBsonDocument(
                byteArrayOf(19, 0, 0, 0, 16, 120, 0, 1, 0, 0, 0, 16, 120, 0, 2, 0, 0, 0, 0)
            )
        val corrupt = RawBsonDocument(byteArrayOf(5, 0, 0, 0, 9))
        val lazy = BsonDocumentWrapper(Document("x", 1), org.bson.codecs.DocumentCodec())
        for (input in listOf(duplicate, corrupt, lazy, RawBsonDocument.parse("{x:1}"))) {
            val result = BsonComparison.compare(input, doc("{x:1}"))
            assertEquals(BsonComparisonState.INVALID, result.state, result.toString())
            assertTrue(result.problem!!.contains("plain BsonDocument"))
            assertEquals(
                BsonComparisonState.INVALID,
                BsonComparison.compare(BsonArray(listOf(input)), BsonArray(listOf(doc("{x:1}"))))
                    .state,
            )
        }
    }

    @Test
    fun negativeAndRelativeDecimalTolerancesRetainSignsAndBoundaries() {
        fun decimal(value: String) = BsonDecimal128(Decimal128.parse(value))
        equal(decimal("-1.0"), decimal("-1.00"), tolerance("", "0"))
        different(decimal("-1"), decimal("1"), policy = tolerance("", "1.9"))
        equal(decimal("-100"), decimal("-101"), tolerance("", "0", "0.01"))
        different(decimal("-100"), decimal("-102"), policy = tolerance("", "0", "0.01"))
        equal(decimal("-0.00"), decimal("0.0"), tolerance("", "0"))
        different(
            BsonDecimal128(Decimal128.NaN),
            BsonDecimal128(Decimal128.NEGATIVE_NaN),
            policy = tolerance("", "100"),
        )
    }

    @Test
    fun workBudgetAlsoCoversComparisonCandidateBuildingAndMatching() {
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compare(
                    doc("{v:1}"),
                    doc("{v:1}"),
                    limits = BsonComparisonLimits(maximumWork = 5),
                )
                .state,
        )
        val rows = listOf(doc("{v:1}"), doc("{v:2}"))
        // Validation costs 8 nodes; four pair comparisons cost another 8. Neither
        // candidate construction nor matching may continue beyond the shared budget.
        for (budget in listOf(15, 16)) {
            val result =
                BsonComparison.compareResults(
                    rows,
                    rows,
                    ResultOrder.UNORDERED,
                    limits = BsonComparisonLimits(maximumWork = budget),
                )
            assertEquals(BsonComparisonState.INVALID, result.state, result.toString())
            assertTrue(result.problem!!.contains("work limit"))
        }
    }

    @Test
    fun legacyResultOrderingAndNumericRepresentationsHaveExplicitDispositions() {
        val a = doc("{rows:[{v:1},{v:2}]}")
        val b = doc("{rows:[{v:2},{v:1}]}")
        assertThrows(AssertionError::class.java) {
            JSONAssert.assertEquals(a.toJson(), b.toJson(), true)
        }
        JSONAssert.assertEquals(a.toJson(), b.toJson(), false)
        val minusZero = BsonDocument("v", BsonDouble(-0.0))
        val plusZero = BsonDocument("v", BsonDouble(0.0))
        JSONAssert.assertEquals(minusZero.toJson(), plusZero.toJson(), true)
        different(minusZero, plusZero)
        val one = BsonDocument("v", BsonDecimal128(Decimal128.parse("1.0")))
        val two = BsonDocument("v", BsonDecimal128(Decimal128.parse("1.00")))
        assertThrows(AssertionError::class.java) {
            JSONAssert.assertEquals(one.toJson(), two.toJson(), true)
        }
        different(one, two)
    }

    @Test
    fun smallMultisetsAgreeWithAnExhaustiveIndependentAssignmentOracle() {
        val triples =
            (0..2).flatMap { a -> (0..2).flatMap { b -> (0..2).map { c -> listOf(a, b, c) } } }
        val permutations =
            listOf(
                listOf(0, 1, 2),
                listOf(0, 2, 1),
                listOf(1, 0, 2),
                listOf(1, 2, 0),
                listOf(2, 0, 1),
                listOf(2, 1, 0),
            )
        val policy = tolerance("/v", "1")
        for (left in triples) for (right in triples) {
            val possible =
                permutations.any { p ->
                    left.indices.all { i -> kotlin.math.abs(left[i] - right[p[i]]) <= 1 }
                }
            val result =
                BsonComparison.compareResults(
                    left.map { BsonDocument("v", BsonInt32(it)) },
                    right.map { BsonDocument("v", BsonInt32(it)) },
                    ResultOrder.UNORDERED,
                    policy,
                )
            assertEquals(
                if (possible) BsonComparisonState.EQUAL else BsonComparisonState.DIFFERENT,
                result.state,
                "$left vs $right: $result",
            )
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun javaNullValuesAreInvalidRatherThanBsonNull() {
        val nullArray = BsonArray()
        nullArray.add(null)
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compare(nullArray, BsonArray()).state,
        )
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compare(null, BsonNull.VALUE).state,
        )
        val nullRows = listOf<BsonDocument?>(null) as List<BsonDocument>
        assertEquals(
            BsonComparisonState.INVALID,
            BsonComparison.compareResults(nullRows, emptyList()).state,
        )
        equal(BsonArray(listOf(BsonNull.VALUE)), BsonArray(listOf(BsonNull.VALUE)))
    }
}
