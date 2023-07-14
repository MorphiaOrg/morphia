package dev.morphia.internal;

import dev.morphia.Datastore;

public class DatastoreHolder {
    public static final ThreadLocal<Datastore> holder = new ThreadLocal<>();
}
