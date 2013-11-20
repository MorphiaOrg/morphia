package org.mongodb.morphia.aggregation;

public class Matcher {
    private String field;
    private String operation;
    private Object operand;

    public Matcher(final String field) {

        this.field = field;
    }

    public Matcher(final String operation, final Object operand) {
        this.operation = operation;
        this.operand = operand;
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

    public static Matcher and(final Matcher... matches) {
        return new Matcher("$and", matches);
    }
}
