package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * A polymorphic root with an id, decoded through its own codec whatever subtype the document names.
 */
@Entity("harness_pets")
public abstract class HarnessPet {
    @Id
    private ObjectId id;
    private String name;

    protected HarnessPet() {
    }

    protected HarnessPet(ObjectId id, String name) {
        this.id = id;
        this.name = name;
    }

    public static HarnessPet sample() {
        return new HarnessDog(new ObjectId("650000000000000000000201"), "rex", 3);
    }
}
