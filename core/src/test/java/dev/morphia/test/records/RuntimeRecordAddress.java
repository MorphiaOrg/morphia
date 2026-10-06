package dev.morphia.test.records;

import dev.morphia.annotations.Entity;

/**
 * An embedded record outside the packages listed in {@code morphia.packages}, so the critter mapper generates its model at runtime.
 */
@Entity
public record RuntimeRecordAddress(String street, String city) {
}
