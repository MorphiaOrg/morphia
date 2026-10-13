package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;

import org.bson.types.ObjectId;

@Entity("harness_renames")
public class HarnessRenames {
    @Id
    private ObjectId id;
    @Property("n")
    private String name;
    @AlsoLoad("oldEmail")
    private String email;
    @LoadOnly
    private String legacy;
    @Transient
    private String scratch;
    private transient String alsoScratch;

    public HarnessRenames() {
    }

    public static HarnessRenames sample() {
        HarnessRenames entity = new HarnessRenames();
        entity.id = new ObjectId("650000000000000000000009");
        entity.name = "renamed";
        entity.email = "a@example.com";
        entity.legacy = "never written";
        entity.scratch = "not mapped";
        entity.alsoScratch = "not mapped either";
        return entity;
    }

    public ObjectId getId() {
        return id;
    }

    public String getLegacy() {
        return legacy;
    }
}
