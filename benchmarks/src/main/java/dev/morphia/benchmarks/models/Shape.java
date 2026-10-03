package dev.morphia.benchmarks.models;

import dev.morphia.annotations.Entity;

/**
 * Root of a small polymorphic hierarchy; decoding requires a discriminator lookup.
 */
@Entity
public abstract class Shape {
    private String color;

    protected Shape() {
    }

    protected Shape(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
