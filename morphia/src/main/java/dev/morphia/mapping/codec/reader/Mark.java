package dev.morphia.mapping.codec.reader;

import org.bson.BsonReaderMark;

public class Mark implements BsonReaderMark {
    private final Stage stage;

    public Mark(final Stage stage) {
        this.stage = stage;
    }

    public void reset() {
/*
        super.reset();
        FlattenedDocumentReader.this.currentValue = currentValue;
        FlattenedDocumentReader.this.setContext(context);
        context.reset();
*/
    }
}
