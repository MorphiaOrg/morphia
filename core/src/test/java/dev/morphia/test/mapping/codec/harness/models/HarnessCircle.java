package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

@Entity
public class HarnessCircle extends HarnessShape {
    private double radius;

    public HarnessCircle() {
    }

    public HarnessCircle(String label, double radius) {
        super(label);
        this.radius = radius;
    }
}
