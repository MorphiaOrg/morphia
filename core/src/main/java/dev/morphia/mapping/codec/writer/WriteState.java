package dev.morphia.mapping.codec.writer;

import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.List;
import java.util.StringJoiner;

abstract class WriteState {
    private final DocumentWriter writer;
    private final WriteState previous;

    WriteState(DocumentWriter writer) {
        this.writer = writer;
        this.previous = writer.state(this);
    }

    //    @Override
    //    public String toString() {
    //        return stateToString("");
    //    }

    //    void apply(Object value) {
    //        throw new UnsupportedOperationException();
    //    }

    protected abstract String state();

    //    protected String stateToString(String s) {
    //        return s;
    //    }

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
            return value.toString();
        }
    }

    WriteState array() {
        return new ArrayState(getWriter());
    }

    WriteState document() {
        return new DocumentState(getWriter());
    }

    final void end() {
        getWriter().previous();
    }

    DocumentWriter getWriter() {
        return writer;
    }

    WriteState name(String name) {
        throw new UnsupportedOperationException();
        //        return new NameState(getWriter(), name, document);
    }

    <P extends WriteState> P previous() {
        return (P) previous;
    }

    void value(Object value) {
        throw new IllegalStateException(Sofia.notInValidState("value", state()));
    }
}
