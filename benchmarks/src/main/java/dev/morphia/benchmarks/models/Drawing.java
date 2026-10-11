package dev.morphia.benchmarks.models;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * Holds a list of polymorphic {@link Shape}s.
 */
@Entity("drawings")
public class Drawing {
    @Id
    private ObjectId id;
    private String title;
    private List<Shape> shapes;

    public Drawing() {
    }

    public static Drawing sample() {
        Drawing drawing = new Drawing();
        drawing.id = new ObjectId();
        drawing.title = "Shapes";
        drawing.shapes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            drawing.shapes.add(new Circle("red", i + 1.5));
            drawing.shapes.add(new Square("green", i + 2.0));
            drawing.shapes.add(new Rectangle("blue", i + 1.0, i + 3.0));
        }
        return drawing;
    }

    public ObjectId getId() {
        return id;
    }

    public List<Shape> getShapes() {
        return shapes;
    }
}
