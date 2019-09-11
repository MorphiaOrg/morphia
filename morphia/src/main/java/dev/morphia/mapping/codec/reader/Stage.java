package dev.morphia.mapping.codec.reader;

import dev.morphia.sofia.Sofia;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.List;
import java.util.StringJoiner;

class Stage {
    DocumentReader reader;
    ReaderIterator iterator;
    private final String name;
    private final Object value;
    Stage nextStage;

    Stage(final DocumentReader reader, final ReaderIterator iterator) {
        this.reader = reader;
        this.iterator = iterator;
        name = null;
        value = null;
        if (iterator.hasNext()) {
            processNextStages(new DocumentEndStage(reader), iterator);
        }
    }

    private void processNextStages(final Stage endStage, final ReaderIterator iterator) {
        next(endStage);
        Stage current = this;
        while (iterator.hasNext()) {
            current = current.next(iterator.next());
        }
        reader.nextStage(nextStage);
    }

    Stage(final DocumentReader reader, final String name, final Object value) {
        this.reader = reader;
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
        return value == null ? null : reader.getBsonType(value);
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

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                   .add("currentBsonType=" + getCurrentBsonType())
                   .add("name='" + name + "'")
                   .add("value=" + value)
                   .toString();
    }

    static class InitialStage extends Stage {

        InitialStage(final DocumentReader reader, final DocumentIterator documentIterator) {
            super(reader, documentIterator);
            DocumentStartStage startStage = new DocumentStartStage(reader, iterator);
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

        DocumentStartStage(final DocumentReader reader, final ReaderIterator iterator) {
            super(reader, iterator);
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
            if(iterator.hasNext()) {
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
        void endDocument() {
            end(Sofia.notDocumentEnd());
        }

        @Override
        BsonType getCurrentBsonType() {
            return BsonType.END_OF_DOCUMENT;
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
