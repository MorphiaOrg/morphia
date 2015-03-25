package org.mongodb.morphia.generics.model;

public class ChildEmbedded extends FatherEmbedded {

    private String childField;

    public ChildEmbedded() {
    }

    public ChildEmbedded(final String childField) {
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
        if (!(o instanceof ChildEmbedded)) {
            return false;
        }

        final ChildEmbedded that = (ChildEmbedded) o;

        return childField.equals(that.childField);

    }
}
