package dev.morphia.test.models;

import dev.morphia.Datastore;
import dev.morphia.PostLoadListener;

/**
 * The critter generator tests' {@code @EntityListeners} class. It lives outside {@code dev.morphia.critter}, which the critter
 * class loader loads its own copies of, so the annotation the generated model reads names the same class.
 */
public class ExampleListener implements PostLoadListener<Object> {
    @Override
    public void postLoad(Object entity, Datastore datastore) {
    }
}
