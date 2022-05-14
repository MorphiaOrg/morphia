package dev.morphia.mapping.codec.writer;

import com.mongodb.lang.Nullable;

import static dev.morphia.mapping.codec.writer.PendingValue.SLUG;
import static java.lang.String.format;

class NameState extends WriteState {
    private final String name;
    private ValueState value = SLUG;

    NameState(DocumentWriter writer, String name, WriteState previous) {
        super(writer, previous);
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return format("%s: %s", name, value);
    }

    @Nullable
    public Object value() {
        return value.value();
    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    WriteState array() {
        value = new ArrayState(getWriter(), this);
        return value;
    }

    @Override
    WriteState document() {
        value = new DocumentState(getWriter(), this);
        return value;
    }

    @Override
    void done() {
        end();
    }

    @Override
    void value(Object value) {
        this.value = new SingleValue(getWriter(), value, this);
        this.value.end();
    }
}
