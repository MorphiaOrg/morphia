package dev.morphia.aggregation.experimental.expressions;

import static java.util.Arrays.asList;

public class SetExpression extends Expression {
    protected SetExpression(final String operation, final Object value) {
        super(operation, value);
    }

    /**
     * Takes two or more arrays and returns an array that contains the elements that appear in every input array.
     *
     * @param arrays expressions that resolve to arrays
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/setIntersection $setIntersection
     */
    public static SetExpression setIntersection(final Expression... arrays) {
        return new SetExpression("$setIntersection", asList(arrays));
    }
}
