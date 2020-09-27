package dev.morphia.mapping.codec;

import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

class ArrayState extends ValueState {
    private final List<Object> list = new ArrayList<>();

    ArrayState(DocumentWriter writer) {
        super(writer);
    }

    @Override
    protected String stateToString(String s) {
        return previous().stateToString(list.toString())
               + (!s.equals("") ? ", " + s : "");
    }

    @Override
    void end() {
        previous().apply(list);
    }

    @Override
    protected void apply(Object value) {
        list.add(value);
        getWriter().state(this);
    }

    @Override
    WriteState value(Object value) {
        list.add(value);
        return this;
    }
}

class DocumentState extends WriteState {
    private final Document document;

    DocumentState(DocumentWriter writer) {
        super(writer);
        document = new Document();
    }

    DocumentState(DocumentWriter writer, Document seed) {
        super(writer);
        document = seed != null ? seed : new Document();
    }

    public DocumentState applyValue(String name, Object value) {
        if (value instanceof Document && document.get(name) instanceof Document) {
            Document extant = (Document) document.get(name);
            extant.putAll((Document) value);
        } else {
            document.put(name, value);
        }
        getWriter().state(this);
        return this;
    }

    @Override
    protected String stateToString(String s) {
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
    NameState name(String name) {
        return new NameState(getWriter(), name);
    }

    Document getDocument() {
        return document;
    }
}

class NameState extends WriteState {
    private final String name;

    NameState(DocumentWriter writer, String name) {
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
        return new ArrayState(getWriter());
    }

    @Override
    WriteState document() {
        return new DocumentState(getWriter());
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
    WriteState value(Object value) {
        return previous().applyValue(name, value);
    }

    void apply(Object value) {
        previous().applyValue(name, value);
    }
}

class RootState extends WriteState {

    private final Document document;

    RootState(DocumentWriter writer) {
        super(writer);
        document = null;
    }

    RootState(DocumentWriter writer, Document seed) {
        super(writer);
        document = seed;
    }

    @Override
    protected String stateToString(String s) {
        return s;
    }

    @Override
    WriteState document() {
        return new DocumentState(getWriter(), document);
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
    NameState name(String name) {
        return new NameState(getWriter(), name);
    }
}

class ValueState extends WriteState {
    ValueState(DocumentWriter writer) {
        super(writer);
    }

    @Override
    protected String state() {
        return "value";
    }
}

abstract class WriteState {
    private final DocumentWriter writer;
    private final WriteState previous;

    WriteState(DocumentWriter writer) {
        this.writer = writer;
        this.previous = writer.state(this);
    }

    @Override
    public String toString() {
        return stateToString("");
    }

    void apply(Object value) {
        throw new UnsupportedOperationException();
    }

    protected abstract String state();

    protected String stateToString(String s) {
        return s;
    }

    WriteState array() {
        return new ArrayState(getWriter());
    }

    WriteState document() {
        return new DocumentState(getWriter());
    }

    void end() {
        getWriter().state(previous());
    }

    DocumentWriter getWriter() {
        return writer;
    }

    WriteState name(String name) {
        return new NameState(getWriter(), name);
    }

    <P extends WriteState> P previous() {
        return (P) previous;
    }

    WriteState value(Object value) {
        throw new IllegalStateException(Sofia.notInValidState("value", state()));
    }
}
