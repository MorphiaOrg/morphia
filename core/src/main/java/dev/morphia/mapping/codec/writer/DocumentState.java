package dev.morphia.mapping.codec.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

/**
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class DocumentState extends ValueState<Map<String, Object>> {
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

    @Override
    void end() {
        finished = new Document();
        values.forEach(v -> merge(finished, v.name(), v.value()));
        super.end();
    }

    @Override
    NameState name(String name) {
        NameState state = new NameState(getWriter(), name, this);
        values.add(state);
        return state;
    }

    /**
     * Adds a value to a document combining it with any existing value for the same key rather than replacing it. Operators (keys starting
     * with {@code $}) are combined using {@code $and} while subdocuments are merged recursively. Existing subdocuments are copied rather
     * than modified in place.
     *
     * @param target the document to update
     * @param key    the key to set
     * @param value  the value to add
     */
    static void merge(Document target, String key, @Nullable Object value) {
        if (target.containsKey(key)) {
            Object current = target.get(key);
            if (key.startsWith("$")) {
                target.remove(key);
                merge(target, "$and", List.of(new Document(key, current), new Document(key, value)));
                return;
            } else if (current instanceof Document && value instanceof Document) {
                Document merged = new Document((Document) current);
                ((Document) value).forEach((k, v) -> merge(merged, k, v));
                target.put(key, merged);
                return;
            }
        }
        target.put(key, value);
    }
}
