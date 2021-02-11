package dev.morphia.mapping.codec.reader;

import com.mongodb.DBRef;
import com.mongodb.lang.Nullable;
import dev.morphia.sofia.Sofia;
import org.bson.BsonBinary;
import org.bson.BsonType;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

abstract class ReaderState {
    private final DocumentReader reader;
    private ReaderState nextState;

    ReaderState(DocumentReader reader) {
        this.reader = reader;
    }

    @Nullable
    public ReaderState nextState() {
        return nextState;
    }

    public DocumentReader reader() {
        return reader;
    }

    @Override
    public String toString() {
        return getStateName();
    }

    protected ReaderState valueState(Object value) {
        Object unwind = unwind(value);
        if (unwind instanceof Document) {
            return new DocumentState(reader, (Document) unwind);
        } else if (unwind instanceof List) {
            return new ArrayState(reader, (List<?>) unwind);
        }
        return new ValueState(reader, unwind);
    }

    void advance() {
        reader.state(nextState);
    }

    void endArray() {
        throw new IllegalStateException(Sofia.invalidReaderState("endArray", ArrayState.NAME, getStateName()));
    }

    void endDocument() {
        throw new IllegalStateException(Sofia.invalidReaderState("endDocument", DocumentState.NAME, getStateName()));
    }

    abstract BsonType getCurrentBsonType();

    abstract String getStateName();

    String name() {
        throw new IllegalStateException(Sofia.invalidReaderState("readName", NameState.NAME, getStateName()));
    }

    void next(ReaderState next) {
        ReaderState old = nextState;
        nextState = next;
        if (nextState != null) {
            nextState.nextState = old;
        }
    }

    void skipName() {
        throw new IllegalStateException(Sofia.invalidReaderState("skipName", NameState.NAME, getStateName()));
    }

    void skipValue() {
        throw new IllegalStateException(Sofia.invalidReaderState("skipValue", ValueState.NAME, getStateName()));
    }

    void startArray() {
        throw new IllegalStateException(Sofia.invalidReaderState("startArray", ArrayState.NAME, getStateName()));
    }

    void startDocument() {
        throw new IllegalStateException(Sofia.invalidReaderState("startDocument", DocumentState.NAME, getStateName()));
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

    <T> T value() {
        throw new IllegalStateException(Sofia.invalidReaderState("read value", ValueState.NAME, getStateName()));
    }
}
