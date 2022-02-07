package dev.morphia.mapping.codec.writer;

import com.mongodb.lang.Nullable;

abstract class ValueState<T> extends WriteState {
    ValueState(DocumentWriter writer, WriteState previous) {
        super(writer, previous);
    }

    ValueState() {
    }

    @Nullable
    public abstract T value();
}
