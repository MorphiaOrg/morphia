package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity(value = "harness_no_discriminator", useDiscriminator = false)
public class HarnessNoDiscriminator {
    @Id
    private ObjectId id;
    private String name;

    public HarnessNoDiscriminator() {
    }

    public static HarnessNoDiscriminator sample() {
        HarnessNoDiscriminator entity = new HarnessNoDiscriminator();
        entity.id = new ObjectId("650000000000000000000006");
        entity.name = "plain";
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
