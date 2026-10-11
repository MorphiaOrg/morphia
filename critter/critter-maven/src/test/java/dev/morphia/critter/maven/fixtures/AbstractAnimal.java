package dev.morphia.critter.maven.fixtures;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity
public abstract class AbstractAnimal {
    @Id
    private ObjectId id;
    private String name;
}
