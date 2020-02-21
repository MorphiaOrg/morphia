package dev.morphia.mapping.codec.reader;

import dev.morphia.sofia.Sofia;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.List;
import java.util.StringJoiner;

class ReaderState {
    private final String name;
    private final Object value;
    private DocumentReader reader;
    private ReaderIterator iterator;
    private ReaderState nextReaderState;

    ReaderState(final DocumentReader reader, final ReaderIterator iterator) {
        this.reader = reader;
        this.iterator = iterator;
        name = null;
        value = null;
        if (iterator.hasNext()) {
            processNextStages(new DocumentEndReaderState(reader), iterator);
        }
    }

    public ReaderIterator getIterator() {
        return iterator;
    }

    protected void nextStage(final ReaderState next) {
        nextReaderState = next;
    }

    public ReaderState getNextReaderState() {
        return nextReaderState;
    }

    private void processNextStages(final ReaderState endReaderState, final ReaderIterator iterator) {
        next(endReaderState);
        ReaderState current = this;
        while (iterator.hasNext()) {
            current = current.next(iterator.next());
        }
        reader.nextStage(nextReaderState);
    }

    ReaderState next(final ReaderState next) {
        ReaderState old = nextReaderState;
        nextReaderState = next;
        nextReaderState.nextReaderState = old;
        return next;
    }

    ReaderState(final DocumentReader reader, final String name, final Object value) {
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

    ReaderState advance() {
        return reader.nextStage(nextReaderState);
    }

    void startDocument() {
        if (!(value instanceof Document)) {
            throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
        }
        if (!(nextReaderState instanceof DocumentStartReaderState)) {
            processNextStages(new DocumentEndReaderState(reader), new DocumentIterator(reader, ((Document) value).entrySet().iterator()));
            next(new DocumentStartReaderState(reader, ArrayIterator.empty())).advance();
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
        if (!(nextReaderState instanceof ListValueReaderState)) {
            processNextStages(new ListEndReaderState(reader), new ArrayIterator(reader, ((List) value).iterator()));
        }
    }

    void endArray() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(List.class, getCurrentBsonType()));
    }

    void endDocument() {
        throw new BsonInvalidOperationException(Sofia.invalidBsonOperation(Document.class, getCurrentBsonType()));
    }

    static class InitialReaderState extends ReaderState {

        InitialReaderState(final DocumentReader reader, final DocumentIterator documentIterator) {
            super(reader, documentIterator);
            DocumentStartReaderState startStage = new DocumentStartReaderState(reader, getIterator());
            startStage.nextStage(getNextReaderState());
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

    static class DocumentStartReaderState extends ReaderState {

        DocumentStartReaderState(final DocumentReader reader, final ReaderIterator iterator) {
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

    static class ListValueReaderState extends ReaderState {
        ListValueReaderState(final DocumentReader reader, final Object value) {
            super(reader, null, value);
        }

        @Override
        String name() {
            throw new BsonInvalidOperationException(Sofia.cannotReadName());
        }
    }

    private static class EndReaderState extends ReaderState {
        EndReaderState(final DocumentReader reader) {
            super(reader, ArrayIterator.empty());
        }

        void end(final String message) {
            if (getIterator().hasNext()) {
                throw new BsonInvalidOperationException(message);
            }
            advance();
        }
    }

    static class DocumentEndReaderState extends EndReaderState {
        DocumentEndReaderState(final DocumentReader reader) {
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

    static class ListEndReaderState extends EndReaderState {
        ListEndReaderState(final DocumentReader reader) {
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
