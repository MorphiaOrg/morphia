package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.critter.sources.other.PackageBase;

/**
 * A subclass inheriting package-private fields from a superclass in another package.
 */
@Entity
public class PackageChildExample extends PackageBase {
    private int size;
}
