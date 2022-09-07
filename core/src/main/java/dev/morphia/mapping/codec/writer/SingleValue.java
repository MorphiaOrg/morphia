package dev.morphia.mapping.codec.writer;

class SingleValue extends ValueState<Object> {

    private final Object value;

    SingleValue(DocumentWriter writer, Object value, WriteState previous) {
        super(writer, previous);
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    protected String state() {
        return "single";
    }
}
