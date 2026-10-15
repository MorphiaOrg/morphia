package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;

import org.bson.types.ObjectId;

/**
 * Counts its lifecycle callbacks, so a subtype encoded or decoded through this type's codec shows whether they ran once.
 */
@Entity("harness_callback_shapes")
public abstract class HarnessCallbackShape {
    @Id
    private ObjectId id;
    @Transient
    private int prePersists;
    @Transient
    private int postPersists;
    @Transient
    private int preLoads;
    @Transient
    private int postLoads;

    protected HarnessCallbackShape() {
    }

    protected HarnessCallbackShape(ObjectId id) {
        this.id = id;
    }

    @PrePersist
    void prePersist() {
        prePersists++;
    }

    @PostPersist
    void postPersist() {
        postPersists++;
    }

    @PreLoad
    void preLoad() {
        preLoads++;
    }

    @PostLoad
    void postLoad() {
        postLoads++;
    }

    public int getPrePersists() {
        return prePersists;
    }

    public int getPostPersists() {
        return postPersists;
    }

    public int getPreLoads() {
        return preLoads;
    }

    public int getPostLoads() {
        return postLoads;
    }
}
