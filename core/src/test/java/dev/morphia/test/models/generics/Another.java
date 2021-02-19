package dev.morphia.test.models.generics;

public class Another extends EmbeddedType {

    private String anotherField;

    public Another() {
    }

    public Another(String anotherField) {
        this.anotherField = anotherField;
    }

    @Override
    public int hashCode() {
        return anotherField.hashCode();
    }

    @Override
    public boolean equals(Object o) {
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
