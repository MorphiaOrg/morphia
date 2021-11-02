package dev.morphia.test.models.external;

import java.util.Objects;

public class ThirdPartyEmbedded {
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
        if (!(o instanceof ThirdPartyEmbedded)) {
            return false;
        }
        ThirdPartyEmbedded that = (ThirdPartyEmbedded) o;
        return Objects.equals(field, that.field) && Objects.equals(number, that.number);
    }
}
