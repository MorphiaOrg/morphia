package dev.morphia.mapping.codec.reader;

import org.bson.BsonReaderMark;

/**
 * A bookmark for processing Documents
 * @see DocumentReader
 */
public class Mark implements BsonReaderMark {
    private DocumentReader reader;
    private final Stage stage;

    Mark(final DocumentReader reader, final Stage stage) {
        this.reader = reader;
        this.stage = stage;
    }

    /**
     * Resets the reader to place indicated by this Mark
     */
    public void reset() {
        reader.reset(stage);
    }
}
