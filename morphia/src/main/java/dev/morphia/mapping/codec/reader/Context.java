package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.BsonTypeMap;
import dev.morphia.mapping.codec.reader.Stage.InitialStage;
import dev.morphia.sofia.Sofia;
import org.bson.BsonType;
import org.bson.Document;

import java.util.ArrayDeque;
import java.util.List;

public class Context {

    private final ArrayDeque<ReaderIterator> iterators = new ArrayDeque<>();
    Stage stage;
    //    List<Stage> stages = new ArrayList<>();
    //    private int position = 0;
    private final BsonTypeMap typeMap = new BsonTypeMap();

    public Context(final Document document) {
        iterators.add(new DocumentIterator(this, document.entrySet().iterator()));
        stage = new InitialStage(this);
    }

    public Stage iterate() {
        ReaderIterator peek = iterators.peek();
        return newStage(peek != null ? peek.next() : null);
    }

    public void iterate(final ReaderIterator iterator) {
        iterators.push(iterator);
    }

    ReaderIterator popIterator() {
        return iterators.pop();
    }

    Stage newStage(final Stage newStage) {
        this.stage.next(newStage);
        this.stage = newStage;
        return newStage;
    }


    Stage nextStage(final Stage newStage) {
        if (newStage.nextStage == null) {
            newStage.next(iterate());
        }

        stage = newStage.nextStage;
        return stage;
    }


    Stage stage() {
        return this.stage;
    }

    void stage(final Stage reset) {
        stage = reset;
    }

    public Mark mark() {
        return new Mark(this, stage());
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

}
