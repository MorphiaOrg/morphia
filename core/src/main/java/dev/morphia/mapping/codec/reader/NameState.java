package dev.morphia.mapping.codec.reader;

import org.bson.BsonType;

/**
 * @morphia.internal
 */
public class NameState extends ReaderState {

    public static final String NAME = "NAME";
    private final String name;

    NameState(DocumentReader reader, String name) {
        super(reader);
        this.name = name;
    }

    @Override
    String name() {
        advance();
        return name;
    }

    @Override
    String getStateName() {
        return NAME;
    }

    @Override
    BsonType getCurrentBsonType() {
        return nextState() != null
               ? nextState().getCurrentBsonType()
               : BsonType.UNDEFINED;
    }

    @Override
    void skipName() {
        advance();
    }
}
