package dev.morphia.mapping.codec.reader;

import com.mongodb.DBRef;
import dev.morphia.sofia.Sofia;
import org.bson.BsonBinary;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonType;
import org.bson.Document;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

class ReaderState {
    private final String name;
    private final Object value;
    private final DocumentReader reader;
    private ReaderIterator iterator;
    private ReaderState nextReaderState;

    ReaderState(DocumentReader reader, ReaderIterator iterator) {
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

    protected void nextStage(ReaderState next) {
        nextReaderState = next;
    }

    public ReaderState getNextReaderState() {
        return nextReaderState;
    }

    private void processNextStages(ReaderState endReaderState, ReaderIterator iterator) {
        next(endReaderState);
        ReaderState current = this;
        while (iterator.hasNext()) {
            current = current.next(iterator.next());
        }
        reader.nextStage(nextReaderState);
    }

    ReaderState next(ReaderState next) {
        ReaderState old = nextReaderState;
        nextReaderState = next;
        nextReaderState.nextReaderState = old;
        return next;
    }

    ReaderState(DocumentReader reader, String name, Object value) {
        this.reader = reader;
        this.name = name;
        this.value = unwind(value);
    }

    private Object unwind(Object value) {
        Object unwind = value;
        if (value instanceof DBRef) {
            DBRef dbRef = (DBRef) value;
            Document document = new Document("$ref", dbRef.getCollectionName())
                                    .append("$id", dbRef.getId());
            if (dbRef.getDatabaseName() != null) {
                document.append("$db", dbRef.getDatabaseName());
            }
            unwind = document;
        } else if (value instanceof UUID) {
            unwind = new BsonBinary((UUID) value);
        }
        return unwind;
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

        InitialReaderState(DocumentReader reader, DocumentIterator documentIterator) {
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

        DocumentStartReaderState(DocumentReader reader, ReaderIterator iterator) {
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
        ListValueReaderState(DocumentReader reader, Object value) {
            super(reader, null, value);
        }

        @Override
        String name() {
            throw new BsonInvalidOperationException(Sofia.cannotReadName());
        }
    }

    private static class EndReaderState extends ReaderState {
        EndReaderState(DocumentReader reader) {
            super(reader, ArrayIterator.empty());
        }

        void end(String message) {
            if (getIterator().hasNext()) {
                throw new BsonInvalidOperationException(message);
            }
            advance();
        }
    }

    static class DocumentEndReaderState extends EndReaderState {
        DocumentEndReaderState(DocumentReader reader) {
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
        ListEndReaderState(DocumentReader reader) {
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
