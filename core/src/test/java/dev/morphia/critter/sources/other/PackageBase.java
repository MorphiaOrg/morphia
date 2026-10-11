package dev.morphia.critter.sources.other;

import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * A superclass whose package-private field can't be reached from subclasses in other packages.
 */
public class PackageBase {
    @Id
    ObjectId id;
    String label;
}
