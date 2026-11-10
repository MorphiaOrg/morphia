package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;

/**
 * A subclass whose generation weaves {@link ConstructorExample} again, for its inherited private fields.
 */
@Entity
public class ConstructorChildExample extends ConstructorExample {
    private String label;

    public ConstructorChildExample() {
        super(0);
    }
}
