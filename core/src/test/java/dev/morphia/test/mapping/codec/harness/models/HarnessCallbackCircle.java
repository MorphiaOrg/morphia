package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

import org.bson.types.ObjectId;

@Entity("harness_callback_shapes")
public class HarnessCallbackCircle extends HarnessCallbackShape {
    private double radius;

    public HarnessCallbackCircle() {
    }

    public HarnessCallbackCircle(ObjectId id, double radius) {
        super(id);
        this.radius = radius;
    }
}
