package dev.morphia.audits.offline

import dev.morphia.audits.repairs.ActionShape
import dev.morphia.audits.repairs.FormEquivalence
import dev.morphia.audits.repairs.FormRule
import dev.morphia.audits.semantics.BsonComparison
import dev.morphia.audits.semantics.BsonComparisonState
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FormEquivalenceTest {
    private fun values(json: String): List<BsonValue> =
        BsonDocument.parse("{v: $json}").getArray("v").values

    private fun canonical(json: String, shape: ActionShape, vararg rules: FormRule) =
        BsonArray(FormEquivalence.canonical(values(json), shape, rules.toSet()))

    /** BsonDocument.equals ignores key order; the strict comparator does not. */
    private fun equal(left: BsonArray, right: BsonArray) =
        BsonComparison.compare(left, right).state == BsonComparisonState.EQUAL

    private fun same(left: String, right: String, shape: ActionShape, vararg rules: FormRule) {
        assertTrue(equal(canonical(left, shape, *rules), canonical(right, shape, *rules)))
        // Without the rule, the two spellings are different.
        assertFalse(equal(canonical(left, shape), canonical(right, shape)))
    }

    private fun different(left: String, right: String, shape: ActionShape, rule: FormRule) =
        assertFalse(equal(canonical(left, shape, rule), canonical(right, shape, rule)))

    @Test
    fun conditionDocumentIsTheArrayForm() {
        same(
            """[{${'$'}project: {d: {${'$'}cond: {if: {${'$'}gte: ["${'$'}q", 250]}, then: 30, else: 20}}}}]""",
            """[{${'$'}project: {d: {${'$'}cond: [{${'$'}gte: ["${'$'}q", 250]}, 30, 20]}}}]""",
            ActionShape.PIPELINE,
            FormRule.COND_ARRAY,
        )
        // Only the exact if/then/else document is rewritten.
        different(
            """[{${'$'}project: {d: {${'$'}cond: {if: true, then: 1}}}}]""",
            """[{${'$'}project: {d: {${'$'}cond: [true, 1]}}}]""",
            ActionShape.PIPELINE,
            FormRule.COND_ARRAY,
        )
    }

    @Test
    fun equalityShorthandOnlyInPredicates() {
        same(
            """[{tags: {${'$'}eq: ["A", "B"]}, "item.name": {${'$'}eq: "ab"}}]""",
            """[{tags: ["A", "B"], "item.name": "ab"}]""",
            ActionShape.FIND,
            FormRule.EQ_SHORTHAND,
        )
        same(
            """[{${'$'}match: {${'$'}or: [{qty: {${'$'}eq: 20}}]}}]""",
            """[{${'$'}match: {${'$'}or: [{qty: 20}]}}]""",
            ActionShape.PIPELINE,
            FormRule.EQ_SHORTHAND,
        )
        // {f: {$eq: /re/}} matches the regex value itself; {f: /re/} is a pattern match.
        different(
            """[{f: {${'$'}eq: /a/}}]""",
            """[{f: /a/}]""",
            ActionShape.FIND,
            FormRule.EQ_SHORTHAND,
        )
        // An operator document under $eq is a literal; unwrapped it would become operators.
        different(
            """[{f: {${'$'}eq: {${'$'}gt: 1}}}]""",
            """[{f: {${'$'}gt: 1}}]""",
            ActionShape.FIND,
            FormRule.EQ_SHORTHAND,
        )
        // The aggregation $eq expression is not a predicate.
        different(
            """[{${'$'}project: {e: {${'$'}eq: ["${'$'}a", 1]}}}]""",
            """[{${'$'}project: {e: ["${'$'}a", 1]}}]""",
            ActionShape.PIPELINE,
            FormRule.EQ_SHORTHAND,
        )
    }

    @Test
    fun regularExpressionSpellingsAreOneMatch() {
        // The JSON reader turns {$regex: "p", $options: "o"} into a regex literal (legacy extended
        // JSON), so the anchor keeps the pattern and options apart.
        val anchor = """[{sku: {${'$'}regex: /^ABC/, ${'$'}options: "mi"}}]"""
        listOf("""[{sku: /^ABC/mi}]""", """[{sku: {${'$'}regex: /^ABC/mi}}]""").forEach {
            same(it, anchor, ActionShape.FIND, FormRule.REGEX_FORM)
        }
        // Options in both places are rejected by the server, so they are not merged.
        different(
            """[{sku: {${'$'}regex: /^ABC/i, ${'$'}options: "m"}}]""",
            anchor,
            ActionShape.FIND,
            FormRule.REGEX_FORM,
        )
    }

    @Test
    fun multipleOperatorsOnOneFieldAreAnAnd() {
        same(
            """[{${'$'}match: {${'$'}or: [{score: {${'$'}gt: 70, ${'$'}lt: 90}}]}}]""",
            """[{${'$'}match: {${'$'}or: [{${'$'}and: [{score: {${'$'}gt: 70}}, {score: {${'$'}lt: 90}}]}]}}]""",
            ActionShape.PIPELINE,
            FormRule.FIELD_OPERATORS_AND,
        )
        // $elemMatch must apply all conditions to one element; it is not split.
        different(
            """[{s: {${'$'}elemMatch: {${'$'}gt: 1, ${'$'}lt: 3}}}]""",
            """[{${'$'}and: [{s: {${'$'}elemMatch: {${'$'}gt: 1}}}, {s: {${'$'}elemMatch: {${'$'}lt: 3}}}]}]""",
            ActionShape.FIND,
            FormRule.FIELD_OPERATORS_AND,
        )
    }

    @Test
    fun projectionSpellings() {
        same(
            """[{${'$'}project: {item: 1, _id: 0}}]""",
            """[{${'$'}project: {_id: 0, item: 1}}]""",
            ActionShape.PIPELINE,
            FormRule.PROJECT_ID_FIRST,
        )
        same(
            """[{${'$'}project: {_id: false, a: true}}]""",
            """[{${'$'}project: {_id: 0, a: 1}}]""",
            ActionShape.PIPELINE,
            FormRule.PROJECTION_FLAGS,
        )
        // Other fields keep their order: computed fields appear in specification order.
        different(
            """[{${'$'}project: {a: "${'$'}x", b: "${'$'}y"}}]""",
            """[{${'$'}project: {b: "${'$'}y", a: "${'$'}x"}}]""",
            ActionShape.PIPELINE,
            FormRule.PROJECT_ID_FIRST,
        )
        // A boolean inside an expression is a value, not a projection flag.
        different(
            """[{${'$'}project: {a: {${'$'}literal: true}}}]""",
            """[{${'$'}project: {a: {${'$'}literal: 1}}}]""",
            ActionShape.PIPELINE,
            FormRule.PROJECTION_FLAGS,
        )
    }

    @Test
    fun singleArgumentListsAndUnwindPaths() {
        same(
            """[{${'$'}project: {a: {${'$'}isArray: ["${'$'}x"]}}}]""",
            """[{${'$'}project: {a: {${'$'}isArray: "${'$'}x"}}}]""",
            ActionShape.PIPELINE,
            FormRule.SINGLE_ARGUMENT_ARRAY,
        )
        // [[1]] is an array literal argument: unwrapping it would change the answer.
        different(
            """[{${'$'}project: {a: {${'$'}isArray: [[1]]}}}]""",
            """[{${'$'}project: {a: {${'$'}isArray: [1]}}}]""",
            ActionShape.PIPELINE,
            FormRule.SINGLE_ARGUMENT_ARRAY,
        )
        // In a predicate, $size is the query operator; [2] is not its argument list.
        different(
            """[{f: {${'$'}size: [2]}}]""",
            """[{f: {${'$'}size: 2}}]""",
            ActionShape.FIND,
            FormRule.SINGLE_ARGUMENT_ARRAY,
        )
        same(
            """[{${'$'}unwind: "${'$'}sizes"}]""",
            """[{${'$'}unwind: {path: "${'$'}sizes"}}]""",
            ActionShape.PIPELINE,
            FormRule.UNWIND_PATH,
        )
    }

    @Test
    fun namedArgumentsKeepNestedOrder() {
        same(
            """[{${'$'}group: {_id: 1, g: {${'$'}bottomN: {output: "${'$'}s", n: 1, sortBy: {s: -1, t: 1}}}}}]""",
            """[{${'$'}group: {_id: 1, g: {${'$'}bottomN: {n: 1, output: "${'$'}s", sortBy: {s: -1, t: 1}}}}}]""",
            ActionShape.PIPELINE,
            FormRule.NAMED_ARGUMENTS,
        )
        // A sort specification is ordered: its keys are never sorted.
        different(
            """[{${'$'}group: {_id: 1, g: {${'$'}bottomN: {n: 1, sortBy: {s: -1, t: 1}}}}}]""",
            """[{${'$'}group: {_id: 1, g: {${'$'}bottomN: {n: 1, sortBy: {t: 1, s: -1}}}}}]""",
            ActionShape.PIPELINE,
            FormRule.NAMED_ARGUMENTS,
        )
        // Operators outside the allowlist keep their key order.
        different(
            """[{${'$'}group: {count: {${'$'}sum: 1}, _id: 1}}]""",
            """[{${'$'}group: {_id: 1, count: {${'$'}sum: 1}}}]""",
            ActionShape.PIPELINE,
            FormRule.NAMED_ARGUMENTS,
        )
    }

    @Test
    fun literalContentIsData() {
        FormRule.values().forEach {
            different(
                """[{${'$'}project: {a: {${'$'}literal: {${'$'}cond: {if: 1, then: 2, else: 3}}}}}]""",
                """[{${'$'}project: {a: {${'$'}literal: {${'$'}cond: [1, 2, 3]}}}}]""",
                ActionShape.PIPELINE,
                it,
            )
        }
    }

    @Test
    fun inputsAreNotModified() {
        val input =
            values("""[{${'$'}project: {a: {${'$'}cond: {if: true, then: 1, else: 2}}, _id: 0}}]""")
        val before = BsonArray(input.map { it.clone() })
        FormEquivalence.canonical(input, ActionShape.PIPELINE, FormRule.values().toSet())
        assertTrue(equal(before, BsonArray(input)))
    }

    private fun BsonValue.clone(): BsonValue = BsonDocument("v", this).clone()["v"]!!
}
