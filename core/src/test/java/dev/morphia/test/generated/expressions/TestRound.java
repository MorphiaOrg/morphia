package dev.morphia.test.generated.expressions;

import dev.morphia.test.OperatorCase;
import dev.morphia.test.OperatorCaseRunner;
import dev.morphia.test.TestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.round;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestRound extends TestBase {
    private final OperatorCaseRunner runner = new OperatorCaseRunner(this);

    @Test
    @DisplayName("main")
    public void roundExpressionOperator() {
        runner.aggregate(
                OperatorCase.of("/dev/morphia/test/generated/fixtures/expressions/round/roundExpressionOperator/scenario.json"),
                (aggregation) -> aggregation.pipeline(project().include("roundedValue", round("$value", 1))));
    }
}
