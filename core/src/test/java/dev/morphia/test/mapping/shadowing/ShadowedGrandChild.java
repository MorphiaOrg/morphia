package dev.morphia.test.mapping.shadowing;

import dev.morphia.annotations.Entity;

@Entity
public class ShadowedGrandChild extends ShadowedChild {
    private ShadowedGrandChild shadowed;

    public ShadowedGrandChild getShadowed() {
        return shadowed;
    }

    public void setShadowed(ShadowedGrandChild shadowed) {
        this.shadowed = shadowed;
    }
}
