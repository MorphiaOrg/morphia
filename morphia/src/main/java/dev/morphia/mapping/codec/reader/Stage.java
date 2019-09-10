package dev.morphia.mapping.codec.reader;

import dev.morphia.sofia.Sofia;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.List;
import java.util.StringJoiner;

class Stage {
    Context context;
    ReaderIterator iterator;
    private final String name;
    private final Object value;
    Stage nextStage;

    Stage(final Context context, final ReaderIterator iterator) {
        this.context = context;
        this.iterator = iterator;
        name = null;
        value = null;
        if (iterator.hasNext()) {
            processNextStages(new DocumentEndStage(context), iterator);
        }
    }

    private void processNextStages(final Stage endStage, final ReaderIterator iterator) {
        next(endStage);
        Stage current = this;
        while (iterator.hasNext()) {
            current = current.next(iterator.next());
        }
        context.nextStage(nextStage);
    }

    Stage(final Context context, final String name, final Object value) {
        this.context = context;
        this.name = name;
        this.value = value;
    }

    Stage next(final Stage next) {
        Stage old = nextStage;
        nextStage = next;
        nextStage.nextStage = old;
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
        return context.nextStage(nextStage);
    }

    void startDocument() {
        if (!(value instanceof Document)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
        }
        if (!(nextStage instanceof DocumentStartStage)) {
            processNextStages(new DocumentEndStage(context), new DocumentIterator(context, ((Document) value).entrySet().iterator()));
            next(new DocumentStartStage(context, ArrayIterator.empty())).advance();
        } else {
            advance();
        }
    }

    void startArray() {
        if (!(value instanceof List)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
        }
        if (!(nextStage instanceof ListValueStage)) {
            processNextStages(new ListEndStage(context), new ArrayIterator(context, ((List) value).iterator()));
        }
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
                   .add("currentBsonType=" + getCurrentBsonType())
                   .add("name='" + name + "'")
                   .add("value=" + value)
                   .toString();
    }

    static class InitialStage extends Stage {

        InitialStage(final Context context, final DocumentIterator documentIterator) {
            super(context, documentIterator);
            DocumentStartStage startStage = new DocumentStartStage(context, iterator);
            startStage.nextStage = nextStage;
            nextStage = startStage;
        }

        @Override
        void startDocument() {
            advance();
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }
    }

    static class DocumentStartStage extends Stage {

        DocumentStartStage(final Context context, final ReaderIterator iterator) {
            super(context, iterator);
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
            super(context, null, value);
        }

        @Override
        String name() {
            throw new BsonInvalidOperationException(Sofia.cannotReadName());
        }
    }

    private static class EndStage extends Stage {
        EndStage(final Context context) {
            super(context, ArrayIterator.empty());
        }

        void end(final String message) {
            if(iterator.hasNext()) {
                throw new BsonInvalidOperationException(message);
            }
            advance();
        }
    }

    static class DocumentEndStage extends EndStage {
        DocumentEndStage(final Context context) {
            super(context);
        }

        @Override
        void endDocument() {
            end(Sofia.notDocumentEnd());
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.END_OF_DOCUMENT;
        }

    }

    static class ListEndStage extends EndStage {
        ListEndStage(final Context context) {
            super(context);
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.END_OF_DOCUMENT;
        }

        @Override
        void endArray() {
            end(Sofia.notArrayEnd());
        }
    }
}
