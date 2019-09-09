package dev.morphia.mapping.codec.reader;

import dev.morphia.sofia.Sofia;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

class Stage {
    State state;
    Context context;
    private final String name;
    private final Object value;
    Stage nextStage;

    Stage(final State state, final Context context) {
        this.state = state;
        this.context = context;
        name = null;
        value = null;
    }

    Stage(final State state, final Context context, final String name, final Object value) {
        this.state = state;
        this.context = context;
        this.name = name;
        this.value = value;
    }

    Stage next(final Stage next) {
        this.nextStage = next;
        return next;
    }

    BsonType getCurrentBsonType() {
        return value == null ? null : context.getBsonType(value);
    }

    String name() {
        return name;
    }

    <T> T value() {
        advance();
        return (T) value;
    }

    Stage advance() {
        return context.nextStage(this);
    }

    void startDocument() {
        if (!(value instanceof Document)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
        }
        context.iterate(new DocumentIterator(context, ((Document) value).entrySet().iterator()));
        context.newStage(new DocumentStartStage(context));
    }

    void startArray() {
        if(!(value instanceof List)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
        }
        context.iterate(new ArrayIterator(context, ((List) value).iterator()));
        context.iterate();
    }

    void endArray() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
    }

    void endDocument() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                   .add("state=" + state)
                   .add("currentBsonType=" + getCurrentBsonType())
                   .add("name='" + name + "'")
                   .add("value=" + value)
                   .toString();
    }

    static class InitialStage extends Stage {

        InitialStage(final Context context) {
            super(State.INITIAL, context);
        }

        @Override
        void startDocument() {
            context.newStage(new DocumentStartStage(context));
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }
    }

    static class DocumentStartStage extends Stage {

        DocumentStartStage(final Context context) {
            super(State.VALUE, context);
        }

        @Override
        String name() {
            return advance().name();
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }
    }

    static class ListValueStage extends Stage {
        ListValueStage(final Context context, final Object value) {
            super(State.VALUE, context, null, value);
        }

        @Override
        String name() {
            throw new BsonInvalidOperationException(Sofia.cannotReadName());
        }
    }

    private static class EndStage extends Stage {
        EndStage(final State state, final Context context) {
            super(state, context);
        }

        void end(final String message) {
            Iterator iterator = context.popIterator();
            if(iterator.hasNext()) {
                throw new BsonInvalidOperationException(message);
            }
            advance();
        }
    }

    static class DocumentEndStage extends EndStage {
        DocumentEndStage(final Context context) {
            super(State.END_OF_DOCUMENT, context);
        }

        @Override
        void endDocument() {
            end(Sofia.notDocumentEnd());
        }

    }

    static class ListEndStage extends EndStage {
        ListEndStage(final Context context) {
            super(State.END_OF_ARRAY, context);
        }

        @Override
        void endArray() {
            end(Sofia.notArrayEnd());
        }
    }
}
