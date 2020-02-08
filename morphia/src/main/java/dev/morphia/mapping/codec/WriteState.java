package dev.morphia.mapping.codec;

import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

class ArrayState extends ValueState {
    private List<Object> list = new ArrayList<>();

    ArrayState(final DocumentWriter documentWriter) {
        super(documentWriter);
    }

    @Override
    void end() {
        super.end();
    }

    @Override
    WriteState value(final Object value) {
        list.add(value);
        return this;
    }
}

class DocumentState extends WriteState {
    private final Document document;

    DocumentState(final DocumentWriter documentWriter) {
        super(documentWriter);
        document = new Document();
    }

    DocumentState(final DocumentWriter documentWriter, final Document seed) {
        super(documentWriter);
        document = seed != null ? seed : new Document();
    }

    public DocumentState applyValue(final String name, final Object value) {
        if (value instanceof Document && document.get(name) instanceof Document) {
            Document extant = (Document) document.get(name);
            extant.putAll((Document) value);
        } else {
            document.put(name, value);
        }
        documentWriter.state(this);
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
        if (previous() instanceof NameState) {
            ((NameState) previous()).apply(document);
        }
    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    NameState name(final String name) {
        return new NameState(documentWriter, name);
    }

    Document getDocument() {
        return document;
    }
}

class NameState extends WriteState {
    private String name;

    NameState(final DocumentWriter documentWriter, final String name) {
        super(documentWriter);
        this.name = name;
    }

    @Override
    public String stateToString(String downstream) {
        return previous().stateToString(name + ": "
                                        + (!downstream.equals("") ? downstream : "<pending>"));
    }

    @Override
    WriteState array() {
        return new ArrayState(documentWriter);
    }

    @Override
    WriteState document() {
        return new DocumentState(documentWriter);
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

    RootState(final DocumentWriter documentWriter) {
        super(documentWriter);
        document = null;
    }

    RootState(final DocumentWriter documentWriter, final Document seed) {
        super(documentWriter);
        document = seed;
    }

    @Override
    protected String stateToString(final String s) {
        return s;
    }

    @Override
    WriteState document() {
        return new DocumentState(documentWriter, document);
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
        return new NameState(documentWriter, name);
    }

    Document getDocument() {
        return document;
    }
}

class ValueState extends WriteState {
    ValueState(final DocumentWriter documentWriter) {
        super(documentWriter);
    }

    @Override
    protected String state() {
        return "value";
    }
}

abstract class WriteState {
    final DocumentWriter documentWriter;
    private final WriteState previous;

    WriteState(final DocumentWriter documentWriter) {
        this.documentWriter = documentWriter;
        this.previous = documentWriter.state(this);
    }

    @Override
    public String toString() {
        return stateToString("");
    }

    protected abstract String state();

    protected String stateToString(final String s) {
        return s;
    }

    WriteState array() {
        throw new IllegalStateException(Sofia.notInValidState("value", state()));
    }

    WriteState document() {
        throw new IllegalStateException(Sofia.notInValidState("value", state()));
    }

    void end() {
        documentWriter.state(previous());
    }

    WriteState name(final String name) {
        throw new IllegalStateException(Sofia.notInValidState("name", state()));
    }

    <P extends WriteState> P previous() {
        return (P) previous;
    }

    WriteState value(final Object value) {
        throw new IllegalStateException(Sofia.notInValidState("value", state()));
    }
}