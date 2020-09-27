package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.reader.ReaderState.DocumentEndReaderState;
import dev.morphia.mapping.codec.reader.ReaderState.ListValueReaderState;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * @morphia.internal
 */
interface ReaderIterator extends Iterator<ReaderState> {
}

/**
 * @morphia.internal
 */
class DocumentIterator implements ReaderIterator {
    private final DocumentReader reader;
    private final Iterator<Entry<String, Object>> iterator;

    DocumentIterator(DocumentReader reader, Iterator<Entry<String, Object>> iterator) {
        this.reader = reader;
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public ReaderState next() {
        ReaderState readerState = null;
        try {
            Entry<String, Object> next = iterator.next();
            if (next != null) {
                readerState = new ReaderState(reader, next.getKey(), next.getValue());
            }
        } catch (NoSuchElementException e) {
            readerState = new DocumentEndReaderState(reader);
        }

        return readerState;
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public void forEachRemaining(Consumer action) {
        throw new UnsupportedOperationException();
    }
}

class ArrayIterator implements ReaderIterator {

    private final DocumentReader reader;
    private final Iterator<Object> iterator;

    ArrayIterator() {
        this.reader = null;
        this.iterator = null;
    }

    ArrayIterator(DocumentReader reader, Iterator<Object> iterator) {
        this.reader = reader;
        this.iterator = iterator;
    }

    static ArrayIterator empty() {
        return new ArrayIterator();
    }

    @Override
    public boolean hasNext() {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public ReaderState next() {
        return new ListValueReaderState(reader, iterator.next());
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
