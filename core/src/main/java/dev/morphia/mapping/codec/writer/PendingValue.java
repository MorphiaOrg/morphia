package dev.morphia.mapping.codec.writer;

class PendingValue extends ValueState<Object> {
    static final PendingValue SLUG = new PendingValue();
    static final String PENDING = "<<|>>";

    @Override
    public String toString() {
        return PENDING;
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    protected String state() {
        return "pending value";
    }
}
