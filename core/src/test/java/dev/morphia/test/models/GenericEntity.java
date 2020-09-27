package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class GenericEntity<T> implements SuperMarker {
    @Id
    protected T id;
    protected T test;

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public T getTest() {
        return test;
    }

    public void setTest(T test) {
        this.test = test;
    }
}