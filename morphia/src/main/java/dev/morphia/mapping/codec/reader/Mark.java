package dev.morphia.mapping.codec.reader;

import org.bson.BsonReaderMark;

public class Mark implements BsonReaderMark {
    private DocumentReader reader;
    private final Stage stage;

    public Mark(final DocumentReader reader, final Stage stage) {
        this.reader = reader;
        this.stage = stage;
    }

    public void reset() {
        reader.reset(stage);
    }
}
