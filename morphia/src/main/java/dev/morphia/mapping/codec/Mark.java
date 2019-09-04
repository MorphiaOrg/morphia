package dev.morphia.mapping.codec;

import dev.morphia.mapping.codec.DocumentReader.Context;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonContextType;
import org.bson.BsonReaderMark;
import org.bson.BsonType;

class Mark implements BsonReaderMark {
    private final State state;
    private final Context parentContext;
    private final BsonContextType contextType;
    private final BsonType currentBsonType;
    private final String currentName;
    private DocumentReader reader;

    protected Mark(DocumentReader reader) {
        state = reader.getState();
        parentContext = reader.getContext().getParentContext();
        contextType = reader.getContext().getContextType();
        currentBsonType = reader.getCurrentBsonType();
        currentName = reader.currentName();
        this.reader = reader;
    }

    protected Context getParentContext() {
        return parentContext;
    }

    protected BsonContextType getContextType() {
        return contextType;
    }

    public void reset() {
        reader.setState(state);
        reader.setCurrentBsonType(currentBsonType);
        reader.setCurrentName(currentName);
    }
}
