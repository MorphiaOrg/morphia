package dev.morphia.test;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.MathExpressions.round;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OperatorCaseRunnerTest extends TestBase {
    private static final OperatorCase ROUND = OperatorCase
            .of("/dev/morphia/test/runner/roundExpressionOperator/scenario.json");

    private final OperatorCaseRunner runner = new OperatorCaseRunner(this);

    @Test
    public void aDifferentOperationFailsSerialization() {
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.aggregate(ROUND, (aggregation) -> aggregation.pipeline(project().include("roundedValue",
                        round("$value", 2)))));
        assertTrue(failure.getMessage().contains("serialization differs"), failure.getMessage());
    }

    @Test
    public void stagesNamingAnotherDatabaseAreRejected() {
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.aggregate(OperatorCase.of(
                        "/dev/morphia/test/runner/crossDatabase/scenario.json"),
                        (aggregation) -> aggregation));
        assertTrue(failure.getMessage().contains("a stage naming another database"), failure.getMessage());
    }

    @Test
    public void anAlternativeNamingAnotherDatabaseIsRejected() {
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.aggregate(OperatorCase.of(
                        "/dev/morphia/test/runner/crossDatabaseAlternative/scenario.json"),
                        (aggregation) -> aggregation));
        assertTrue(failure.getMessage().contains("a stage naming another database"), failure.getMessage());
    }

    @Test
    public void onlyNamespaceArgumentsNameADatabase() {
        assertFalse(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$match: {db: 'sales'}}")));
        assertFalse(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$project: {db: 1}}")));
        assertFalse(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$out: 'copy'}")));
        assertTrue(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$out: {db: 'other', coll: 'copy'}}")));
        assertTrue(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$merge: {into: {db: 'other', coll: 'copy'}}}")));
        assertTrue(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$lookup: {from: {db: 'other', coll: 'c'}, as: 'x'}}")));
        assertTrue(OperatorCaseRunner.namesADatabase(
                BsonDocument.parse("{$facet: {a: [{$unionWith: {db: 'other', coll: 'c'}}]}}")));
        assertFalse(OperatorCaseRunner.namesADatabase(BsonDocument.parse("{$project: {x: {$literal: {$out: {db: 'sales'}}}}}")));
    }

    @Test
    public void resultsMustSatisfyThePrintedResults() {
        // mongosh prints a Long as Long("4"), so a stored Long cannot be the printed 4.
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.aggregate(OperatorCase.of(
                        "/dev/morphia/test/runner/printedMismatch/scenario.json"),
                        (aggregation) -> aggregation.pipeline(project().include("v"))));
        assertTrue(failure.getMessage().contains("results differ from the printed results at /0/v"), failure.getMessage());
    }

    @Test
    public void aWritesReadIsTheOneCompared() {
        // The page prints the whole collection after the update; with its read, that is what both sides return.
        runner.update(OperatorCase.of("/dev/morphia/test/runner/readAfterUpdate/scenario.json"),
                (query) -> query.filter(ne("grades", 100)), inc("grades.$[]", 10));
    }

    @Test
    public void aWritesReadReplacesTheFiltersQuery() {
        // After the update the filter's $size has no array to read, so querying it again would fail; only the read runs.
        runner.update(OperatorCase.of("/dev/morphia/test/runner/readAfterUnset/scenario.json"),
                (query) -> query.filter(expr(gt(size("$tags"), 0))), unset("tags"));
    }

    @Test
    public void withoutAReadAnUpdateReturnsWhatItsFilterMatches() {
        // The same update without its read: the filter no longer matches the document whose grades reached 100.
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.update(OperatorCase.of("/dev/morphia/test/runner/filterAfterUpdate/scenario.json"),
                        (query) -> query.filter(ne("grades", 100)), inc("grades.$[]", 10)));
        assertTrue(failure.getMessage().contains("results differ from the documented results"), failure.getMessage());
    }

    @Test
    public void onlyGeneratedScenarioResourcesAreAccepted() {
        assertThrows(IllegalArgumentException.class,
                () -> OperatorCase.of("/dev/morphia/test/aggregation/expressions/abs/absoluteValue/action.json"));
        assertThrows(IllegalArgumentException.class,
                () -> OperatorCase.of("/dev/morphia/test/aggregation/expressions/abs/scenario.json"));
    }
}
