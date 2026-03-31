package dev.morphia.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("critter_test")
public class CritterMapperTestEntity {
    @Id
    private ObjectId id;
    private String name;
    private int value;

    public CritterMapperTestEntity() {
    }
}
