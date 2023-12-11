package dev.morphia.test.aggregation.expressions;

import java.util.List;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.SetExpressions.anyElementTrue;
import static dev.morphia.aggregation.expressions.SetExpressions.setDifference;
import static dev.morphia.aggregation.expressions.SetExpressions.setEquals;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.expressions.SetExpressions.setIsSubset;
import static dev.morphia.aggregation.expressions.SetExpressions.setUnion;
import static java.util.Arrays.asList;

public class SetExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testAnyElementTrue() {
        assertAndCheckDocShape("{ $anyElementTrue: [ [ true, false ] ] }", anyElementTrue(value(asList(true, false))), true);
        assertAndCheckDocShape("{ $anyElementTrue: [ [ [ false ] ] ] }", anyElementTrue(value(asList(asList(false)))), true);
        assertAndCheckDocShape("{ $anyElementTrue: [ [ null, false, 0 ] ] }",
                anyElementTrue(value(asList(null, false, 0))), false);
        assertAndCheckDocShape("{ $anyElementTrue: [ [ ] ] }",
                anyElementTrue(value(List.of())), false);
    }

    @Test
    public void testSetDifference() {
        assertAndCheckDocShape("{ $setDifference: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
                setDifference(value(asList("a", "b", "a")), value(asList("b", "a"))), asList());

        assertAndCheckDocShape("{ $setDifference: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
                setDifference(value(asList("a", "b")), value(asList(asList("a", "b")))), asList("a", "b"));
    }

    @Test
    public void testSetEquals() {
        assertAndCheckDocShape("{ $setEquals: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
                setEquals(value(asList("a", "b", "a")), value(asList("b", "a"))), true);

        assertAndCheckDocShape("{ $setEquals: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
                setEquals(value(asList("a", "b")), value(asList(asList("a", "b")))), false);
    }

    @Test
    public void testSetIntersection() {
        assertAndCheckDocShape("{ $setIntersection: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
                setIntersection(value(asList("a", "b", "a")), value(asList("b", "a"))), asList("a", "b"));

        assertAndCheckDocShape("{ $setIntersection: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
                setIntersection(value(asList("a", "b")), value(asList(asList("a", "b")))), asList());
    }

    @Test
    public void testSetIsSubset() {
        assertAndCheckDocShape("{ $setIsSubset: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
                setIsSubset(value(asList("a", "b", "a")), value(asList("b", "a"))), true);

        assertAndCheckDocShape("{ $setIsSubset: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
                setIsSubset(value(asList("a", "b")), value(asList(asList("a", "b")))), false);
    }

    @Test
    public void testSetUnion() {
        assertAndCheckDocShape("{ $setUnion: [ [ 'a', 'b', 'a' ], [ 'b', 'a' ] ] }",
                setUnion(value(asList("a", "b", "a")), value(asList("b", "a"))), asList("a", "b"));

        assertAndCheckDocShape("{ $setUnion: [ [ 'a', 'b' ], [ [ 'a', 'b' ] ] ] }",
                setUnion(value(asList("a", "b")), value(asList(asList("a", "b")))), asList("a", "b", asList("a", "b")));
    }
}
