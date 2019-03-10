package dev.morphia.generics.model;

public class ChildEmbedded extends FatherEmbedded {

    private String childField;

    public ChildEmbedded() {
    }

    public ChildEmbedded(final String childField) {
        this.childField = childField;
    }

    @Override
    public int hashCode() {
        return childField != null ? childField.hashCode() : 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChildEmbedded that = (ChildEmbedded) o;

        return !(childField != null ? !childField.equals(that.childField) : that.childField != null);

    }
}
