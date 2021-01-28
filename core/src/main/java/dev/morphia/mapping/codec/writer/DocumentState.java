package dev.morphia.mapping.codec.writer;

import org.bson.Document;

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
    public String toString() {
        return toString(document);
    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    NameState name(String name) {
        return new NameState(getWriter(), name, document);
    }

    Document getDocument() {
        return document;
    }
}
