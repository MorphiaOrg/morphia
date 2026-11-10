package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

import org.bson.types.ObjectId;

@Entity
public class HarnessDog extends HarnessPet {
    private int barks;

    public HarnessDog() {
    }

    public HarnessDog(ObjectId id, String name, int barks) {
        super(id, name);
        this.barks = barks;
    }
}
