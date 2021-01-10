package dev.morphia.mapping.codec.reader;

import org.bson.BsonType;

class DocumentEndState extends ReaderState {
    public static final String NAME = "END_DOCUMENT";

    DocumentEndState(DocumentReader reader) {
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
    void endDocument() {
        advance();
    }

}
