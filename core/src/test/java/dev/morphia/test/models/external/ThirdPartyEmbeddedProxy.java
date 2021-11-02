package dev.morphia.test.models.external;

import dev.morphia.annotations.experimental.ExternalEntity;

@ExternalEntity(target = ThirdPartyEmbedded.class)
public class ThirdPartyEmbeddedProxy {
    public String field;
    public Long number;
}
