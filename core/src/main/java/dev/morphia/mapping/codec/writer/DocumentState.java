package dev.morphia.mapping.codec.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import org.bson.Document;

import static java.util.List.of;

class DocumentState extends ValueState<Map<String, Object>> {
    private final List<NameState> values = new ArrayList<>();
    private Document finished;

    DocumentState(DocumentWriter writer, WriteState previous) {
        super(writer, previous);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "{", finished != null ? "}" : "");
        values.forEach(v -> joiner.add(String.valueOf(v)));
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

    @SuppressWarnings("unchecked")
    private Document andTogether(Document doc, String key, @Nullable Object additional) {
        if (additional != null) {
            Document newSubdoc = new Document(key, additional);
            var extant = doc.remove(key);
            List<Document> and = (List<Document>) doc.get("$and");
            if (and != null) {
                and.add(newSubdoc);
            } else {
                and = new ArrayList<>();
                and.addAll(of(new Document(key, extant), newSubdoc));
                doc.put("$and", and);
                return newSubdoc;
            }
        }
        return doc;
    }

    @Override
    void end() {
        finished = new Document();
        values.forEach(v -> {
            Object doc = finished.get(v.name());
            if (doc == null) {
                finished.put(v.name(), v.value());
            } else {
                andTogether(finished, v.name(), v.value());
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
