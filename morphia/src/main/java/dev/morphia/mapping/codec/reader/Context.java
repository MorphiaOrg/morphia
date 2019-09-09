package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.BsonTypeMap;
import dev.morphia.mapping.codec.reader.Stage.InitialStage;
import dev.morphia.sofia.Sofia;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonType;
import org.bson.Document;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public class Context {

    private FlattenedDocumentReader flattenedDocumentReader;
    private final ArrayDeque<Iterator> iterators = new ArrayDeque<>();
    List<Stage> stages = new ArrayList<>();
    private int position = 0;
    private final BsonTypeMap typeMap = new BsonTypeMap();

    public Context(final FlattenedDocumentReader flattenedDocumentReader, final Document document) {
        this.flattenedDocumentReader = flattenedDocumentReader;
        iterators.add(document.entrySet().iterator());
        stages.add(new InitialStage(this));
    }

    public void startDocument() {
        stage().startDocument();
    }

    public void endArray() {
        stage().endArray();
    }

    public void endDocument() {
        stage().endDocument();
    }

    public void startArray() {
        stage().startArray();
    }

    public void iterate(final Iterator<Entry<String, Object>> iterator) {
        iterators.push(iterator);
    }

    Iterator popIterator() {
        return iterators.pop();
    }

    @SuppressWarnings("unchecked")
    <T> T next() {
        Iterator peek = iterators.peek();
        Object next = peek != null ? peek.next() : null;
        return (T) next;
    }

    Stage nextStage() {
        return position < stages.size() - 1 ? stages.get(++position) : null;
    }

    Stage newStage(final Stage stage) {
        position++;
        stages.add(stage);
        return stage;
    }

    Stage stage() {
        if (position < stages.size()) {
            return stages.get(position);
        } else {
            throw new IllegalStateException();
        }
    }

    public Mark mark() {
        return new Mark(stage());
    }

/*
    public void reset() {
        if (documentIterator != null) {
            documentIterator.reset();
        } else {
            arrayIterator.reset();
        }
    }
*/

    public Object getNextValue() {
        throw new UnsupportedOperationException();
    }

    BsonType getBsonType(final Object o) {
        BsonType bsonType = typeMap.get(o.getClass());
        if (bsonType == null) {
            if (o instanceof List) {
                bsonType = BsonType.ARRAY;
            } else {
                throw new IllegalStateException(Sofia.unknownBsonType(o.getClass()));
            }
        }
        return bsonType;
    }

    public String getCurrentName() {
        return stage().name();
    }

    public <T> T getCurrentValue() {
        return stage().value();
    }

    public BsonType getCurrentBsonType() {
        return stage().getCurrentBsonType();
    }
}
