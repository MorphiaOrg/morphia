package dev.morphia.test.mapping.codec.harness.models;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity("harness_drawings")
public class HarnessDrawing {
    @Id
    private ObjectId id;
    private HarnessShape main;
    private HarnessCircle circle;
    private List<HarnessShape> shapes;

    public HarnessDrawing() {
    }

    public static HarnessDrawing sample() {
        HarnessDrawing entity = new HarnessDrawing();
        entity.id = new ObjectId("650000000000000000000005");
        entity.main = new HarnessSquare("main", 2);
        entity.circle = new HarnessCircle("declared", 1);
        entity.shapes = List.of(new HarnessCircle("c", 3), new HarnessSquare("s", 4));
        return entity;
    }

    public ObjectId getId() {
        return id;
    }
}
