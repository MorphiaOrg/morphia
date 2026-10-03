package dev.morphia.benchmarks.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;

import org.bson.types.ObjectId;

/**
 * An entity with lifecycle callbacks, exercising the lifecycle encoder/decoder path.
 */
@Entity("lifecycle")
public class LifecycleEntity {
    @Id
    private ObjectId id;
    private String name;
    private long version;
    @Transient
    private boolean loaded;

    public LifecycleEntity() {
    }

    public static LifecycleEntity sample() {
        LifecycleEntity entity = new LifecycleEntity();
        entity.id = new ObjectId();
        entity.name = "Lifecycle";
        return entity;
    }

    @PrePersist
    void prePersist() {
        version++;
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
