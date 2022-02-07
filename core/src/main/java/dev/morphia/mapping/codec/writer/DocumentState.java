package dev.morphia.mapping.codec.writer;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

class DocumentState extends ValueState<Map<String, Object>> {
    private final List<NameState> values = new ArrayList<>();
    private Document finished;

    DocumentState(DocumentWriter writer, WriteState previous) {
        super(writer, previous);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "{", finished != null ? "}" : "");
        values.forEach(v -> joiner.add(v.toString()));
        return joiner.toString();
    }

    @Override
    public Map<String, Object> value() {
        return finished;
    }

    @Override
    protected String state() {
        return "document";
    }

    @Override
    void end() {
        finished = new Document();
        values.forEach(v -> {
            Object doc = finished.get(v.name());
            if (doc instanceof Document && v.value() instanceof Document) {
                ((Document) doc).putAll((Document) v.value());
            } else {
                finished.put(v.name(), v.value());
            }
        });
        super.end();
    }

    @Override
    NameState name(String name) {
        NameState state = new NameState(getWriter(), name, this);
        values.add(state);
        return state;
    }
}
