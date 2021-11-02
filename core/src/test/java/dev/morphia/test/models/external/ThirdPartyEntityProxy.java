package dev.morphia.test.models.external;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.experimental.ExternalEntity;

@ExternalEntity(value = "extEnt",
    discriminator = "ext",
    discriminatorKey = "_xt",
    concern = "JOURNALED",
    cap = @CappedAt(count = 123, value = 456),
    target = ThirdPartyEntity.class)
public class ThirdPartyEntityProxy {
    public String field;
    @Id
    public Long number;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Id
    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }
}
