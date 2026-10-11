package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;

/**
 * A subclass inheriting private fields from {@link ShapeExample}.
 */
@Entity
public class CircleExample extends ShapeExample {
    private double radius;

    public double getRadius() {
        return radius;
    }
}
