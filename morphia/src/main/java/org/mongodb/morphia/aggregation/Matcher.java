package org.mongodb.morphia.aggregation;

public class Matcher {
    private final String field;
    private String operation;
    private Object operand;

    public Matcher(final String field) {

        this.field = field;
    }

    public static Matcher match(final String field) {
        return new Matcher(field);
    }

    public String getField() {
        return field;
    }

    public Object getOperand() {
        return operand;
    }

    public String getOperation() {
        return operation;
    }

    public Matcher greaterThanEqual(final Object value) {
        operation = "$gte";
        operand = value;
        return this;
    }
}
