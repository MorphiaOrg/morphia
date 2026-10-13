package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_boxes")
public class HarnessBox<T> {
    @Id
    protected ObjectId id;
    protected T value;

    public HarnessBox() {
    }

    public ObjectId getId() {
        return id;
    }
}
