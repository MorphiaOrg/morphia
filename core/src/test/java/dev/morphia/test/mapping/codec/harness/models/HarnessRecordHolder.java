package dev.morphia.test.mapping.codec.harness.models;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_record_holders")
public class HarnessRecordHolder {
    @Id
    private ObjectId id;
    private HarnessPoint origin;
    private List<HarnessPoint> path;

    public HarnessRecordHolder() {
    }

    public static HarnessRecordHolder sample() {
        HarnessRecordHolder entity = new HarnessRecordHolder();
        entity.id = new ObjectId("650000000000000000000008");
        entity.origin = new HarnessPoint(0, 0);
        entity.path = List.of(new HarnessPoint(1, 2), new HarnessPoint(3, 4));
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
