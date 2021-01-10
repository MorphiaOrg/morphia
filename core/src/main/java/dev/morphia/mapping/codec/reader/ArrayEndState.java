package dev.morphia.mapping.codec.reader;

import org.bson.BsonType;

class ArrayEndState extends ReaderState {
    private static final String NAME = "END_ARRAY";

    ArrayEndState(DocumentReader reader) {
        super(reader);
    }

    @Override
    String getStateName() {
        return NAME;
    }

    @Override
    BsonType getCurrentBsonType() {
        return BsonType.END_OF_DOCUMENT;
    }

    @Override
    void endArray() {
        advance();
    }
}
