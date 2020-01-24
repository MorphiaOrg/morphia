package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Expression.value;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.allElementsTrue;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.anyElementTrue;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setDifference;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setEquals;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setIsSubset;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setUnion;
import static java.util.Arrays.asList;

public class SetExpressionsTest extends ExpressionsTest {

    @Test
    public void testAllElementsTrue() {
        evaluate("{ $allElementsTrue: [ [ true, 1, 'someString' ] ] }",
            allElementsTrue(value(List.of(value(true), value(1), value("someString")))), true);
        evaluate("{ $allElementsTrue: [ [ [ false ] ] ] }",
            allElementsTrue(value(List.of(List.of(false)))), true);
        evaluate("{ $allElementsTrue: [ [ ] ] }",
            allElementsTrue(value(List.of())), true);
        evaluate("{ $allElementsTrue: [ [ null, false, 0 ] ] }",
            allElementsTrue(value(asList(null, false, 0))), false);
    }


    @Test
    public void testAnyElementTrue() {
        evaluate("{ $anyElementTrue: [ [ true, false ] ] }", anyElementTrue(value(asList(true, false))), true);
        evaluate("{ $anyElementTrue: [ [ [ false ] ] ] }", anyElementTrue(value(asList(asList(false)))), true);
        evaluate("{ $anyElementTrue: [ [ null, false, 0 ] ] }",
            anyElementTrue(value(asList(null, false, 0))), false);
        evaluate("{ $anyElementTrue: [ [ ] ] }",
            anyElementTrue(value(List.of())), false);
    }

    @Test
    public void testSetDifference() {
        evaluate("{ $setDifference: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
            setDifference(value(asList("a", "b", "a")), value(asList("b", "a"))), asList());

        evaluate("{ $setDifference: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
            setDifference(value(asList("a", "b")), value(asList(asList("a", "b")))), asList("a", "b"));
    }

    @Test
    public void testSetEquals() {
        evaluate("{ $setEquals: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
            setEquals(value(asList("a", "b", "a")), value(asList("b", "a"))), true);

        evaluate("{ $setEquals: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
            setEquals(value(asList("a", "b")), value(asList(asList("a", "b")))), false);
    }

    @Test
    public void testSetIntersection() {
        evaluate("{ $setIntersection: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
            setIntersection(value(asList("a", "b", "a")), value(asList("b", "a"))), asList("a", "b"));

        evaluate("{ $setIntersection: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
            setIntersection(value(asList("a", "b")), value(asList(asList("a", "b")))), asList());
    }

    @Test
    public void testSetIsSubset() {
        evaluate("{ $setIsSubset: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
            setIsSubset(value(asList("a", "b", "a")), value(asList("b", "a"))), true);

        evaluate("{ $setIsSubset: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
            setIsSubset(value(asList("a", "b")), value(asList(asList("a", "b")))), false);
    }

    @Test
    public void testSetUnion() {
        evaluate("{ $setUnion: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
            setUnion(value(asList("a", "b", "a")), value(asList("b", "a"))), asList("a", "b"));

        evaluate("{ $setUnion: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
            setUnion(value(asList("a", "b")), value(asList(asList("a", "b")))), asList("a", "b", asList("a", "b")));
    }
}
