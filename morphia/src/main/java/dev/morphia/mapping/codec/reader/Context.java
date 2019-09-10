package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.BsonTypeMap;
import dev.morphia.mapping.codec.reader.Stage.InitialStage;
import dev.morphia.sofia.Sofia;
import org.bson.BsonType;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Context {

    private final Stage initial;
    private Stage stage;
    private static final BsonTypeMap TYPE_MAP = new BsonTypeMap();

    public Context(final Document document) {
        stage = new InitialStage(this, new DocumentIterator(this, document.entrySet().iterator()));
        initial = stage;
    }

    public List<Stage> stages() {
        List<Stage> stages = new ArrayList<>();
        Stage current = initial;
        while(current != null) {
            stages.add(current);
            current = current.nextStage;
        }

        return stages;
    }

    Stage nextStage(final Stage nextStage) {
        stage = nextStage;
        return stage;
    }

    Stage stage() {
        return this.stage;
    }

    public Mark mark() {
        return new Mark(this, stage());
    }

    void reset(final Stage bookmark) {
        stage = bookmark;
    }

    BsonType getBsonType(final Object o) {
        BsonType bsonType = TYPE_MAP.get(o.getClass());
        if (bsonType == null) {
            if (o instanceof List) {
                bsonType = BsonType.ARRAY;
            } else {
                throw new IllegalStateException(Sofia.unknownBsonType(o.getClass()));
            }
        }
        return bsonType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Context.class.getSimpleName() + "[", "]")
                   .add("stage=" + stage)
                   .add("stages=" + stages())
                   .toString();
    }
}
