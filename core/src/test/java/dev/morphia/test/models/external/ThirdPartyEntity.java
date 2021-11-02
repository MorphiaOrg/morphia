package dev.morphia.test.models.external;

import java.util.Objects;

public class ThirdPartyEntity {
    public String field;
    public Long number;

    public String getField() {
        return field;
    }

    public ThirdPartyEntity setField(String field) {
        this.field = field;
        return this;
    }

    public Long getNumber() {
        return number;
    }

    public ThirdPartyEntity setNumber(Long number) {
        this.number = number;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ThirdPartyEntity)) {
            return false;
        }
        ThirdPartyEntity that = (ThirdPartyEntity) o;
        return Objects.equals(field, that.field) && Objects.equals(number, that.number);
    }
}
