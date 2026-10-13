package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * Has no no-arg constructor, so it is built through its constructor from the decoded values.
 */
@Entity("harness_constructed")
public class HarnessConstructed {
    @Id
    private final ObjectId id;
    private final String name;
    private final int size;

    public HarnessConstructed(ObjectId id, String name, int size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public static HarnessConstructed sample() {
        return new HarnessConstructed(new ObjectId("650000000000000000000007"), "built", 3);
    }

    public ObjectId getId() {
        return id;
    }
}
