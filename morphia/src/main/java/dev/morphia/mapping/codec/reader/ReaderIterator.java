package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.reader.Stage.DocumentEndStage;
import dev.morphia.mapping.codec.reader.Stage.ListEndStage;
import dev.morphia.mapping.codec.reader.Stage.ListValueStage;
import org.bson.AbstractBsonReader.State;

import java.util.Iterator;
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
                stage = context.newStage(new Stage(State.VALUE, context, next.getKey(), next.getValue()));
            }
        } catch (NoSuchElementException e) {
            stage = context.newStage(new DocumentEndStage(context));

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
        try {
            return context.newStage(new ListValueStage(context, iterator.next()));
        } catch (NoSuchElementException e) {
            return context.newStage(new ListEndStage(context));
        }
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