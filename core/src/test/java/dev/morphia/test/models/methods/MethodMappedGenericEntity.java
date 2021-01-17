package dev.morphia.test.models.methods;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.models.SuperMarker;

@Entity
public class MethodMappedGenericEntity<T> implements SuperMarker {
    private T id;
    private T test;

    @Id
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
