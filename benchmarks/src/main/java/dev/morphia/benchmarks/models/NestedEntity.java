package dev.morphia.benchmarks.models;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity with embedded types two levels deep.
 */
@Entity("nested")
public class NestedEntity {
    @Id
    private ObjectId id;
    private String name;
    private Address home;
    private Address work;
    private Profile profile;

    public NestedEntity() {
    }

    public static NestedEntity sample() {
        NestedEntity entity = new NestedEntity();
        entity.id = new ObjectId();
        entity.name = "Nested";
        entity.home = new Address("1 Main St", "Springfield", "12345", "US");
        entity.work = new Address("500 Market St", "Shelbyville", "67890", "US");
        entity.profile = new Profile("Likes benchmarks", new Address("9 Side Rd", "Ogdenville", "11111", "US"),
                List.of("jmh", "bson", "mongodb"));
        return entity;
    }

    public ObjectId getId() {
        return id;
    }

    public Profile getProfile() {
        return profile;
    }
}
