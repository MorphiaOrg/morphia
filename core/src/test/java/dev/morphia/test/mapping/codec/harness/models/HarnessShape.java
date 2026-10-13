package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

@Entity
public abstract class HarnessShape {
    private String label;

    protected HarnessShape() {
    }

    protected HarnessShape(String label) {
        this.label = label;
    }
}
