package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.reader.Stage.DocumentEndStage;
import dev.morphia.mapping.codec.reader.Stage.ListValueStage;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public interface ReaderIterator extends Iterator<Stage> {
}

class DocumentIterator implements ReaderIterator {
    private DocumentReader reader;
    private Iterator<Entry<String, Object>> iterator;

    DocumentIterator(DocumentReader reader, final Iterator<Entry<String, Object>> iterator) {
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
            if(next != null) {
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

    static ArrayIterator empty() {
        return new ArrayIterator();
    }

    public ArrayIterator() {
        this.reader = null;
        this.iterator = List.of().iterator();
    }

    public ArrayIterator(final DocumentReader reader, final Iterator<Object> iterator) {
        this.reader = reader;
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
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