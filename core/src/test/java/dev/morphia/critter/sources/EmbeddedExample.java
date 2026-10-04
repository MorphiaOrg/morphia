package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

/**
 * An entity without an {@code @Id}, as used for embedded values.
 */
@Entity
public class EmbeddedExample {
    @Property("street_name")
    private String street;
    private int number;

    public String getStreet() {
        return street;
    }

    public int getNumber() {
        return number;
    }
}
