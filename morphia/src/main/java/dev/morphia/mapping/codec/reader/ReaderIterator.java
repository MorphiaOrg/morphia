package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.reader.Stage.DocumentEndStage;
import dev.morphia.mapping.codec.reader.Stage.ListValueStage;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * @morphia.internal
 */
interface ReaderIterator extends Iterator<Stage> {
}

/**
 * @morphia.internal
 */
class DocumentIterator implements ReaderIterator {
    private DocumentReader reader;
    private Iterator<Entry<String, Object>> iterator;

    DocumentIterator(final DocumentReader reader, final Iterator<Entry<String, Object>> iterator) {
        this.reader = reader;
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Stage next() {
        Stage stage = null;
        try {
            Entry<String, Object> next = iterator.next();
            if (next != null) {
                stage = new Stage(reader, next.getKey(), next.getValue());
            }
        } catch (NoSuchElementException e) {
            stage = new DocumentEndStage(reader);
        }

        return stage;
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public void forEachRemaining(final Consumer action) {
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

    ArrayIterator(final DocumentReader reader, final Iterator<Object> iterator) {
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
    public Stage next() {
        return new ListValueStage(reader, iterator.next());
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
