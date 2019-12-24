package dev.morphia.aggregation.experimental.stages;

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

    public static class DateToStringExpression extends Expression {
        private Expression timeZone;
        private Expression onNull;

        public DateToStringExpression(final String format,
                                      final Expression expression) {
            super("$dateToString", format, expression);
        }

        public Expression getOnNull() {
            return onNull;
        }

        public Expression getTimeZone() {
            return timeZone;
        }

        public DateToStringExpression onNull(final Expression onNull) {
            this.onNull = onNull;
            return this;
        }

        public DateToStringExpression timeZone(final Expression timeZone) {
            this.timeZone = timeZone;
            return this;
        }
    }
}
