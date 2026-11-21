package dev.morphia.test;

import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static dev.morphia.aggregation.expressions.MathExpressions.round;
import static dev.morphia.aggregation.stages.Projection.project;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OperatorCaseRunnerTest extends TestBase {
    private static final OperatorCase ROUND = OperatorCase
            .of("/dev/morphia/test/generated/fixtures/expressions/round/roundExpressionOperator/scenario.json");

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
                        "/dev/morphia/test/generated/fixtures/runner/crossDatabase/scenario.json"),
                        (aggregation) -> aggregation));
        assertTrue(failure.getMessage().contains("a stage naming another database"), failure.getMessage());
    }

    @Test
    public void resultsMustSatisfyThePrintedResults() {
        // mongosh prints a Long as Long("4"), so a stored Long cannot be the printed 4.
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.aggregate(OperatorCase.of(
                        "/dev/morphia/test/generated/fixtures/runner/printedMismatch/scenario.json"),
                        (aggregation) -> aggregation.pipeline(project().include("v"))));
        assertTrue(failure.getMessage().contains("results differ from the printed results at /0/v"), failure.getMessage());
    }

    @Test
    public void aConvertedCasesOptionsMustMatchItsScenario() {
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> runner.aggregate(ROUND, new ActionTestOptions().orderMatters(false),
                        (aggregation) -> aggregation.pipeline(project().include("roundedValue", round("$value", 1)))));
        assertTrue(failure.getMessage().contains("options differ from its scenario: [orderMatters]"), failure.getMessage());
    }

    @Test
    public void onlyGeneratedScenarioResourcesAreAccepted() {
        assertThrows(IllegalArgumentException.class,
                () -> OperatorCase.of("/dev/morphia/test/aggregation/expressions/abs/absoluteValue/action.json"));
        assertThrows(IllegalArgumentException.class,
                () -> OperatorCase.of("/dev/morphia/test/aggregation/expressions/abs/scenario.json"));
    }
}
