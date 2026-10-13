package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * Kept apart from {@link HarnessCollections} because critter maps any entity with an array property reflectively.
 */
@Entity("harness_arrays")
public class HarnessArrays {
    @Id
    private ObjectId id;
    private int[] ints;
    private double[] doubles;
    private String[] strings;
    private HarnessAddress[] addresses;

    public HarnessArrays() {
    }

    public static HarnessArrays sample() {
        HarnessArrays entity = new HarnessArrays();
        entity.id = new ObjectId("65000000000000000000000e");
        entity.ints = new int[] { 1, 2, 3 };
        entity.doubles = new double[] { 0.5, 1.5 };
        entity.strings = new String[] { "p", "q" };
        entity.addresses = new HarnessAddress[] { new HarnessAddress("5 Pine St", "Capital City") };
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
