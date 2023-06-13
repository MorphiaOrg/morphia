package dev.morphia.test.models.external;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.test.models.errors.unannotated.external.ThirdPartyEmbedded;

@ExternalEntity(target = ThirdPartyEmbedded.class)
public class ThirdPartyEmbeddedProxy {
    public String field;
    public Long number;
}
