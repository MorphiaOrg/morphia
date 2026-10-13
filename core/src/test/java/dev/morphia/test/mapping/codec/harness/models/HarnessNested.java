package dev.morphia.test.mapping.codec.harness.models;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_nested")
public class HarnessNested {
    @Id
    private ObjectId id;
    private String name;
    private HarnessAddress home;
    private HarnessProfile profile;

    public HarnessNested() {
    }

    public static HarnessNested sample() {
        HarnessNested entity = new HarnessNested();
        entity.id = new ObjectId("650000000000000000000002");
        entity.name = "nested";
        entity.home = new HarnessAddress("1 Main St", "Springfield");
        entity.profile = new HarnessProfile("bio", new HarnessAddress("2 Side St", "Shelbyville"),
                List.of("chess", "rowing"));
        return entity;
    }

    /**
     * @return an entity with empty and null values, which storeNulls and storeEmpties treat differently
     */
    public static HarnessNested sparse() {
        HarnessNested entity = new HarnessNested();
        entity.id = new ObjectId("650000000000000000000003");
        entity.profile = new HarnessProfile(null, new HarnessAddress(null, "x"), new ArrayList<>());
        return entity;
    }

    /**
     * @return an entity with nothing but its id
     */
    public static HarnessNested empty() {
        HarnessNested entity = new HarnessNested();
        entity.id = new ObjectId("65000000000000000000000d");
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
