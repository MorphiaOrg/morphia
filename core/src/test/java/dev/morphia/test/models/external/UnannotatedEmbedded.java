package dev.morphia.test.models.external;

import java.util.Objects;

public class UnannotatedEmbedded {
    public String field;
    public Long number;

    @Override
    public int hashCode() {
        return Objects.hash(field, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnannotatedEmbedded)) {
            return false;
        }
        UnannotatedEmbedded that = (UnannotatedEmbedded) o;
        return Objects.equals(field, that.field) && Objects.equals(number, that.number);
    }
}
