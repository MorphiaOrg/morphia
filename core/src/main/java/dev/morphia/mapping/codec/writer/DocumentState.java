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
        finished = new MergingDocument();
        values.forEach(v -> {
            finished.put(v.name(), v.value());
        });
        finished = new Document(finished);
        super.end();
    }

    @Override
    NameState name(String name) {
        NameState state = new NameState(getWriter(), name, this);
        values.add(state);
        return state;
    }

    private static class MergingDocument extends Document {
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Object put(String key, Object value) {
            if (containsKey(key)) {
                var current = get(key);
                if (current instanceof Document && value instanceof Document) {
                    ((Document) current).putAll((Document) value);
                    return current;
                } else if ((key.equals("$and") || key.equals("$or")) && current instanceof List && value instanceof List) {
                    ((List) current).addAll(((List) value));
                    return current;
                }
            }
            return super.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ?> map) {
            map.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
            super.putAll(map);
        }
    }
}
