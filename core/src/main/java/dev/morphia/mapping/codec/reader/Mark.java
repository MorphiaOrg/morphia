package dev.morphia.mapping.codec.reader;

import org.bson.BsonReaderMark;

/**
 * A bookmark for processing Documents
 * @see DocumentReader
 */
public class Mark implements BsonReaderMark {
    private final DocumentReader reader;
    private final ReaderState readerState;

    Mark(DocumentReader reader, ReaderState readerState) {
        this.reader = reader;
        this.readerState = readerState;
    }

    /**
     * Resets the reader to place indicated by this Mark
     */
    public void reset() {
        reader.reset(readerState);
    }
}
