package dev.morphia.test.mapping.shadowing;

import dev.morphia.annotations.Entity;

@SuppressWarnings("unused")
@Entity(value = "child")
public class ShadowedChild extends ShadowedGrandParent {
    private ShadowedChild shadowed;

    public ShadowedChild getShadowed() {
        return shadowed;
    }

    public void setShadowed(ShadowedChild shadowed) {
        this.shadowed = shadowed;
    }
}
