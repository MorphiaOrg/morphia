package dev.morphia.mapping.codec.reader;

import dev.morphia.sofia.Sofia;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.List;
import java.util.StringJoiner;

class Stage {
    private final String name;
    private final Object value;
    private DocumentReader reader;
    private ReaderIterator iterator;
    private Stage nextStage;

    Stage(final DocumentReader reader, final ReaderIterator iterator) {
        this.reader = reader;
        this.iterator = iterator;
        name = null;
        value = null;
        if (iterator.hasNext()) {
            processNextStages(new DocumentEndStage(reader), iterator);
        }
    }

    public ReaderIterator getIterator() {
        return iterator;
    }

    protected void nextStage(final Stage next) {
        nextStage = next;
    }

    public Stage getNextStage() {
        return nextStage;
    }

    private void processNextStages(final Stage endStage, final ReaderIterator iterator) {
        next(endStage);
        Stage current = this;
        while (iterator.hasNext()) {
            current = current.next(iterator.next());
        }
        reader.nextStage(nextStage);
    }

    Stage next(final Stage next) {
        Stage old = nextStage;
        nextStage = next;
        nextStage.nextStage = old;
        return next;
    }

    Stage(final DocumentReader reader, final String name, final Object value) {
        this.reader = reader;
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                   .add("currentBsonType=" + getCurrentBsonType())
                   .add("name='" + name + "'")
                   .add("value=" + value)
                   .toString();
    }

    String name() {
        return name;
    }

    <T> T value() {
        advance();
        return (T) value;
    }

    Stage advance() {
        return reader.nextStage(nextStage);
    }

    void startDocument() {
        if (!(value instanceof Document)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
        }
        if (!(nextStage instanceof DocumentStartStage)) {
            processNextStages(new DocumentEndStage(reader), new DocumentIterator(reader, ((Document) value).entrySet().iterator()));
            next(new DocumentStartStage(reader, ArrayIterator.empty())).advance();
        } else {
            advance();
        }
    }

    BsonType getCurrentBsonType() {
        return value == null ? null : reader.getBsonType(value);
    }

    void startArray() {
        if (!(value instanceof List)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
        }
        if (!(nextStage instanceof ListValueStage)) {
            processNextStages(new ListEndStage(reader), new ArrayIterator(reader, ((List) value).iterator()));
        }
    }

    void endArray() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
    }

    void endDocument() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
    }

    static class InitialStage extends Stage {

        InitialStage(final DocumentReader reader, final DocumentIterator documentIterator) {
            super(reader, documentIterator);
            DocumentStartStage startStage = new DocumentStartStage(reader, getIterator());
            startStage.nextStage(getNextStage());
            nextStage(startStage);
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }

        @Override
        void startDocument() {
            advance();
        }
    }

    static class DocumentStartStage extends Stage {

        DocumentStartStage(final DocumentReader reader, final ReaderIterator iterator) {
            super(reader, iterator);
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.DOCUMENT;
        }

        @Override
        String name() {
            return advance().name();
        }
    }

    static class ListValueStage extends Stage {
        ListValueStage(final DocumentReader reader, final Object value) {
            super(reader, null, value);
        }

        @Override
        String name() {
            throw new BsonInvalidOperationException(Sofia.cannotReadName());
        }
    }

    private static class EndStage extends Stage {
        EndStage(final DocumentReader reader) {
            super(reader, ArrayIterator.empty());
        }

        void end(final String message) {
            if (getIterator().hasNext()) {
                throw new BsonInvalidOperationException(message);
            }
            advance();
        }
    }

    static class DocumentEndStage extends EndStage {
        DocumentEndStage(final DocumentReader reader) {
            super(reader);
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.END_OF_DOCUMENT;
        }

        @Override
        void endDocument() {
            end(Sofia.notDocumentEnd());
        }

    }

    static class ListEndStage extends EndStage {
        ListEndStage(final DocumentReader reader) {
            super(reader);
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
