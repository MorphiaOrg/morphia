package dev.morphia.mapping.codec;

import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

class ArrayState extends ValueState {
    private List<Object> list = new ArrayList<>();

    ArrayState(final DocumentWriter writer) {
        super(writer);
    }

    @Override
    protected String stateToString(final String s) {
        return previous().stateToString(list.toString()) +
               (!s.equals("") ? ", " + s : "");
    }

    @Override
    void end() {
        previous().apply(list);
    }

    @Override
    protected void apply(final Object value) {
        list.add(value);
        writer.state(this);
    }

    @Override
    WriteState value(final Object value) {
        list.add(value);
        return this;
    }
}

class DocumentState extends WriteState {
    private final Document document;

    DocumentState(final DocumentWriter writer) {
        super(writer);
        document = new Document();
    }

    DocumentState(final DocumentWriter writer, final Document seed) {
        super(writer);
        document = seed != null ? seed : new Document();
    }

    public DocumentState applyValue(final String name, final Object value) {
        if (value instanceof Document && document.get(name) instanceof Document) {
            Document extant = (Document) document.get(name);
            extant.putAll((Document) value);
        } else {
            document.put(name, value);
        }
        writer.state(this);
        return this;
    }

    @Override
    protected String stateToString(final String s) {
        StringJoiner joiner = new StringJoiner(", ", "{ ", " }");
        document.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .forEach(joiner::add);

        if (!"".equals(s)) {
            joiner.add(s);
        }
        return previous().stateToString(joiner.toString());
    }

    @Override
    void end() {
        if (!(previous() instanceof RootState)) {
            previous().apply(document);
        }
    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    NameState name(final String name) {
        return new NameState(writer, name);
    }

    Document getDocument() {
        return document;
    }
}

class NameState extends WriteState {
    private String name;

    NameState(final DocumentWriter writer, final String name) {
        super(writer);
        this.name = name;
    }

    @Override
    public String stateToString(String downstream) {
        return previous().stateToString(name + ": "
                                        + (!downstream.equals("") ? downstream : "<pending>"));
    }

    @Override
    WriteState array() {
        return new ArrayState(writer);
    }

    @Override
    WriteState document() {
        return new DocumentState(writer);
    }

    @Override
    DocumentState previous() {
        return super.previous();
    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    WriteState value(final Object value) {
        return previous().applyValue(name, value);
    }

    void apply(final Object value) {
        previous().applyValue(name, value);
    }
}

class RootState extends WriteState {

    private final Document document;

    RootState(final DocumentWriter writer) {
        super(writer);
        document = null;
    }

    RootState(final DocumentWriter writer, final Document seed) {
        super(writer);
        document = seed;
    }

    @Override
    protected String stateToString(final String s) {
        return s;
    }

    @Override
    WriteState document() {
        return new DocumentState(writer, document);
    }

    @Override
    WriteState previous() {
        throw new IllegalStateException(Sofia.alreadyAtRoot());
    }

    @Override
    protected String state() {
        return "root";
    }

    @Override
    NameState name(final String name) {
        return new NameState(writer, name);
    }

    Document getDocument() {
        return document;
    }
}

class ValueState extends WriteState {
    ValueState(final DocumentWriter writer) {
        super(writer);
    }

    @Override
    protected String state() {
        return "value";
    }
}

abstract class WriteState {
    final DocumentWriter writer;
    private final WriteState previous;

    WriteState(final DocumentWriter writer) {
        this.writer = writer;
        this.previous = writer.state(this);
    }

    @Override
    public String toString() {
        return stateToString("");
    }

    void apply(final Object value) {
        throw new UnsupportedOperationException();
    }

    protected abstract String state();

    protected String stateToString(final String s) {
        return s;
    }

    WriteState array() {
        return new ArrayState(writer);
    }

    WriteState document() {
        return new DocumentState(writer);
    }

    void end() {
        writer.state(previous());
    }

    WriteState name(final String name) {
        return new NameState(writer, name);
    }

    <P extends WriteState> P previous() {
        return (P) previous;
    }

    WriteState value(final Object value) {
        throw new IllegalStateException(Sofia.notInValidState("value", state()));
    }
}