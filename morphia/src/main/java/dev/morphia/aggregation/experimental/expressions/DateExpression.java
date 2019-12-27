package dev.morphia.aggregation.experimental.expressions;

public class DateExpression extends Expression {

    protected DateExpression(final String operation, final String name, final Object value) {
        super(operation, name, value);
    }

    public static DateExpression month(String name, Object value) {
        return new DateExpression("$month", name, value);
    }

    public static DateExpression year(String name, Object value) {
        return new DateExpression("$year", name, value);
    }

    public static DateToStringExpression dateToString(final String format, final Expression expression) {
        return new DateToStringExpression(format, expression);
    }

}
