package dev.morphia.critter.maven.shared;

import dev.morphia.annotations.Entity;

/**
 * An {@code @Entity} interface outside the scanned packages.
 */
@Entity("stored")
public interface Stored {
}
