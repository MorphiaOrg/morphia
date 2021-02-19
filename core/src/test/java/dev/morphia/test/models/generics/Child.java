package dev.morphia.test.models.generics;

public class Child extends EmbeddedType {

    private String childField;

    public Child() {
    }

    public Child(String childField) {
        this.childField = childField;
    }

    @Override
    public int hashCode() {
        return childField != null ? childField.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Child that = (Child) o;

        return !(childField != null ? !childField.equals(that.childField) : that.childField != null);

    }
}
