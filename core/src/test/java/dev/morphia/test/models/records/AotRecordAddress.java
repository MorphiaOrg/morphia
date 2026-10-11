package dev.morphia.test.models.records;

import dev.morphia.annotations.Entity;

/**
 * An embedded record in a package listed in {@code morphia.packages}, so critter-maven generates its model ahead of time.
 */
@Entity
public record AotRecordAddress(String street, String city) {
}
