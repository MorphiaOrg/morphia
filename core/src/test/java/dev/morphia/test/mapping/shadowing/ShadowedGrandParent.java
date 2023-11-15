package dev.morphia.test.mapping.shadowing;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity(value = "grandParent")
public class ShadowedGrandParent {
    @Id
    private ObjectId id;

    private ShadowedGrandParent shadowed;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ShadowedGrandParent getShadowed() {
        return shadowed;
    }

    public void setShadowed(ShadowedGrandParent shadowed) {
        this.shadowed = shadowed;
    }
}
