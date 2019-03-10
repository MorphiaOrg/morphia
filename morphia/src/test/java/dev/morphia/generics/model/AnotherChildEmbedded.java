package dev.morphia.generics.model;

import dev.morphia.annotations.Embedded;

@Embedded
public class AnotherChildEmbedded extends FatherEmbedded {

    private String childField;

    public AnotherChildEmbedded() {
    }

    public AnotherChildEmbedded(final String childField) {
        this.childField = childField;
    }

    @Override
    public int hashCode() {
        return childField.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnotherChildEmbedded)) {
            return false;
        }

        final AnotherChildEmbedded that = (AnotherChildEmbedded) o;

        return childField.equals(that.childField);

    }
}
