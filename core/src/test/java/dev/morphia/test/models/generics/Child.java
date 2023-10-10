package dev.morphia.test.models.generics;

import java.util.Objects;

public class Child extends EmbeddedType {

    private String childField;
    private String first;
    private String last;

    public Child() {
    }

    public Child(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public Child(String childField) {
        this.childField = childField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Child child)) {
            return false;
        }
        return Objects.equals(childField, child.childField) && Objects.equals(first, child.first) &&
               Objects.equals(last, child.last);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childField, first, last);
    }
}
