package dev.morphia.mapping.codec.writer;

public class SingleValue extends ValueState<Object> {

    private final Object value;

    public SingleValue(DocumentWriter writer, Object value, WriteState previous) {
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
