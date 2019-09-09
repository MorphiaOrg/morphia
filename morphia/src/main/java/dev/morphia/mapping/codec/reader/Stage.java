package dev.morphia.mapping.codec.reader;

import dev.morphia.sofia.Sofia;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

class Stage {
    State state;
    Context context;
    private final String name;
    private final Object value;

    public Stage(final State state, final Context context) {
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

    public BsonType getCurrentBsonType() {
        return value == null ? null : context.getBsonType(value);
    }

    public String name() {
        return name;
    }

    public <T> T value() {
        advance();
        return (T) value;
    }

    public Stage advance() {
        Stage stage = context.nextStage();

        if (stage == null) {
            try {
                Entry<String, Object> next = context.next();
                if(next != null) {
                    stage = context.newStage(new Stage(State.VALUE, context, next.getKey(), next.getValue()));
                }
            } catch (NoSuchElementException e) {
                stage = context.newStage(new DocumentEndStage(context));

            }
        }

        return stage;
    }

    public void startDocument() {
        if (!(value instanceof Document)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
        }
        context.iterate(((Document) value).entrySet().iterator());
    }

    public void startArray() {
        if(!(value instanceof List)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
        }
        context.iterate(((List)value).iterator());
        context.newStage(new ListValueStage(context, context.next()));
    }

    public void endArray() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
    }

    public void endDocument() {
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

    static class DocumentStartStage extends Stage {
        public DocumentStartStage(final Context context) {
            super(State.VALUE, context);
        }

        @Override
        public String name() {
            return advance().name();
        }

        @Override
        public BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }
    }

    static class InitialStage extends Stage {

        public InitialStage(final Context context) {
            super(State.INITIAL, context);
        }

        @Override
        public void startDocument() {
            context.newStage(new DocumentStartStage(context));
        }

        @Override
        public BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }
    }

    private static class ListValueStage extends Stage {
        public ListValueStage(final Context context, final Object value) {
            super(State.VALUE, context, null, value);
        }

        @Override
        public Stage advance() {
            Stage stage = context.nextStage();

            if (stage == null) {
                try {
                    return context.newStage(new ListValueStage(context, context.next()));
                } catch (NoSuchElementException e) {
                    return context.newStage(new ListEndStage(context));
                }
            }

            return stage;
        }

    }

    private static class EndStage extends Stage {
        public EndStage(final State state, final Context context) {
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

    private static class DocumentEndStage extends EndStage {
        public DocumentEndStage(final Context context) {
            super(State.END_OF_DOCUMENT, context);
        }

        @Override
        public void endDocument() {
            end(Sofia.notDocumentEnd());
        }

    }

    private static class ListEndStage extends EndStage {
        public ListEndStage(final Context context) {
            super(State.END_OF_ARRAY, context);
        }

        @Override
        public void endArray() {
            end(Sofia.notArrayEnd());
        }
    }
}
