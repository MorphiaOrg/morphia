package dev.morphia.test.mapping.codec.harness.models;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_callback_holders")
public class HarnessCallbackHolder {
    @Id
    private ObjectId id;
    private List<HarnessCallbackShape> shapes = new ArrayList<>();

    public HarnessCallbackHolder() {
    }

    public static HarnessCallbackHolder sample() {
        HarnessCallbackHolder holder = new HarnessCallbackHolder();
        holder.id = new ObjectId("650000000000000000000110");
        holder.shapes.add(new HarnessCallbackCircle(new ObjectId("650000000000000000000111"), 2.5));
        return holder;
    }

    public List<HarnessCallbackShape> getShapes() {
        return shapes;
    }
}
