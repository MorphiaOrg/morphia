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
    private Context context;
    private Iterator<Entry<String, Object>> iterator;

    DocumentIterator(Context context, final Iterator<Entry<String, Object>> iterator) {
        this.context = context;
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
                stage = new Stage(context, next.getKey(), next.getValue());
            }
        } catch (NoSuchElementException e) {
            stage = new DocumentEndStage(context);

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

    private final Context context;
    private final Iterator<Object> iterator;

    static ArrayIterator empty() {
        return new ArrayIterator();
    }

    public ArrayIterator() {
        this.context = null;
        this.iterator = List.of().iterator();
    }

    public ArrayIterator(final Context context, final Iterator<Object> iterator) {
        this.context = context;
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Stage next() {
        return new ListValueStage(context, iterator.next());
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