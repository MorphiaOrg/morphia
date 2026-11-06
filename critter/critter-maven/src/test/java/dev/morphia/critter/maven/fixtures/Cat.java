package dev.morphia.critter.maven.fixtures;

import dev.morphia.annotations.Id;
import dev.morphia.critter.maven.shared.Stored;

import org.bson.types.ObjectId;

/**
 * An entity only through the interface it implements.
 */
public class Cat implements Stored {
    @Id
    private ObjectId id;
    private String name;
}
