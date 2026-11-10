package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Transient;

/**
 * A subtype with a load callback, so its own codec decodes it in a way a plain root's can't pick up part-way.
 */
@Entity
public class HarnessCat extends HarnessPet {
    private int lives;
    @Transient
    private boolean loaded;

    public HarnessCat() {
    }

    @PostLoad
    void postLoad() {
        loaded = true;
    }
}
