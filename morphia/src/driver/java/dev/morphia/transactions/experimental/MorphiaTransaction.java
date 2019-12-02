package dev.morphia.transactions.experimental;

import dev.morphia.experimental.MorphiaSession;

public interface MorphiaTransaction<T> {
    T execute(MorphiaSession session);
}
