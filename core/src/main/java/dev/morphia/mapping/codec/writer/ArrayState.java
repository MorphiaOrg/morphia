package dev.morphia.mapping.codec.writer;

import java.util.ArrayList;
import java.util.List;

class ArrayState extends WriteState {
    private final List<Object> list = new ArrayList<>();

    ArrayState(DocumentWriter writer) {
        super(writer);
    }

    public List<Object> getList() {
        return list;
    }

    @Override
    public String toString() {
        return toString(list);
    }

    @Override
    protected String state() {
        return "array";
    }

    WriteState array() {
        ArrayState arrayState = new ArrayState(getWriter());
        list.add(arrayState.getList());
        return arrayState;
    }

    WriteState document() {
        DocumentState documentState = new DocumentState(getWriter());
        list.add(documentState.getDocument());
        return documentState;
    }

    @Override
    void value(Object value) {
        list.add(value);
    }
}
