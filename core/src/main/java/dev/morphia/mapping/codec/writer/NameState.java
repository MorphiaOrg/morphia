package dev.morphia.mapping.codec.writer;

import org.bson.Document;

class NameState extends WriteState {
    private final String name;
    private final Document document;
    private WriteState value;

    NameState(DocumentWriter writer, String name, Document document) {
        super(writer);
        this.name = name;
        this.document = document;
        if (!document.containsKey(name)) {
            document.put(name, this);
        }

    }

    @Override
    public String toString() {
        return value == null
               ? "<<pending>>"
               : value.toString();
    }

    //    void apply(Object value) {
    //        previous().applyValue(name, value);
    //    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    WriteState array() {
        value = new ArrayState(getWriter());
        document.put(name, ((ArrayState) value).getList());
        return value;
    }

    @Override
    WriteState document() {
        if (document.get(name) instanceof Document) {
            value = new DocumentState(getWriter(), (Document) document.get(name));
        } else {
            value = new DocumentState(getWriter());
            document.put(name, ((DocumentState) value).getDocument());
        }
        return value;
    }

    @Override
    DocumentState previous() {
        return super.previous();
    }

    @Override
    void value(Object value) {
        document.put(name, value);
        end();
    }
}
