package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;

/**
 * A subclass declaring a field with the same name as one in its superclass.
 */
@Entity
public class ShadowingExample extends ShapeExample {
    private String color;
}
