package dev.morphia.test.models.external;

import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = ThirdPartyEmbedded.class)
public class ThirdPartyEmbeddedProxy {
    public String field;
    public Long number;
}
