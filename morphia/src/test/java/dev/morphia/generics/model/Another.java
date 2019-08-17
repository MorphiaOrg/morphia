package dev.morphia.generics.model;

public class Another extends EmbeddedType {

    private String anotherField;

    public Another() {
    }

    public Another(final String anotherField) {
        this.anotherField = anotherField;
    }

    @Override
    public int hashCode() {
        return anotherField.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Another)) {
            return false;
        }

        final Another that = (Another) o;

        return anotherField.equals(that.anotherField);

    }
}
