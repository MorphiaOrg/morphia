package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;

import org.bson.types.ObjectId;

@Entity("harness_lifecycle")
public class HarnessLifecycle {
    @Id
    private ObjectId id;
    private String name;
    private String normalized;
    @Transient
    private boolean loaded;

    public HarnessLifecycle() {
    }

    public static HarnessLifecycle sample() {
        HarnessLifecycle entity = new HarnessLifecycle();
        entity.id = new ObjectId("65000000000000000000000a");
        entity.name = "Lifecycle";
        return entity;
    }

    @PrePersist
    void prePersist() {
        normalized = name == null ? null : name.toLowerCase();
    }

    @PostLoad
    void postLoad() {
        loaded = true;
    }

    public ObjectId getId() {
        return id;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
