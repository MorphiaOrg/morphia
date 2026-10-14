package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.IdField;

@Entity("harness_id_field")
@IdField("key")
public class HarnessIdField {
    private String key;
    private String name;

    public HarnessIdField() {
    }

    public static HarnessIdField sample() {
        HarnessIdField entity = new HarnessIdField();
        entity.key = "key-1";
        entity.name = "named by @IdField";
        return entity;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}
