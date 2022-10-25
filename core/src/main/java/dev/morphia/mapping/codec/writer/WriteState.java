package dev.morphia.mapping.codec.writer;

import java.util.List;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.sofia.Sofia;

import org.bson.Document;

abstract class WriteState {
    private final DocumentWriter writer;
    private final WriteState previous;

    WriteState() {
        writer = null;
        previous = null;
    }

    WriteState(DocumentWriter writer, @Nullable WriteState previous) {
        this.writer = writer;
        this.previous = previous;
        writer.state(this);
    }

    protected abstract String state();

    protected String toString(Object value) {
        if (value instanceof Document) {
            StringJoiner joiner = new StringJoiner(", ", "{ ", " }");
            ((Document) value).entrySet().stream()
                    .map(e -> e.getKey() + ": " + toString(e.getValue()))
                    .forEach(joiner::add);

            return joiner.toString();
        } else if (value instanceof List) {
            StringJoiner joiner = new StringJoiner(", ", "[ ", " ]");
            ((List<?>) value).stream()
                    .map(this::toString)
                    .forEach(joiner::add);

            return joiner.toString();
        } else {
            return String.valueOf(value);
        }
    }

    WriteState array() {
        throw new IllegalStateException(Sofia.cantStartArray(state()) + ". writer: " + getWriter());
    }

    WriteState document() {
        throw new IllegalStateException(Sofia.cantStartDocument(state()) + ". writer: " + getWriter());
    }

    void done() {
    }

    void end() {
        getWriter().state(previous);
        if (previous != null) {
            previous.done();
        }
    }

    DocumentWriter getWriter() {
        return writer;
    }

    WriteState name(String name) {
        throw new IllegalStateException(Sofia.notInValidState("name", state()) + "  writer:  " + getWriter());
    }

    void value(Object value) {
        throw new IllegalStateException(Sofia.notInValidState("value", state()) + "  writer:  " + getWriter());
    }
}
