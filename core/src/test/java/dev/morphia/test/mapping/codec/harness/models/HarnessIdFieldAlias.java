package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.IdField;

/**
 * The {@code @IdField} property also loads from {@code _id}, so renaming it to {@code _id} meets its own alias.
 */
@Entity("harness_id_field_alias")
@IdField("key")
public class HarnessIdFieldAlias {
    @AlsoLoad("_id")
    private String key;

    public String getKey() {
        return key;
    }
}
