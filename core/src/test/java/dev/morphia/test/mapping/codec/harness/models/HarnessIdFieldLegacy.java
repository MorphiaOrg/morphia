package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.IdField;
import dev.morphia.annotations.Property;

/**
 * The {@code @IdField} property's mapped name before the rename is also one of its load names, so it must keep loading
 * from it.
 */
@Entity("harness_id_field_legacy")
@IdField("key")
public class HarnessIdFieldLegacy {
    @Property("legacy")
    @AlsoLoad("legacy")
    private String key;

    public String getKey() {
        return key;
    }
}
