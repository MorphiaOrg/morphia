package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;


@Entity("shapes")
public class Circle extends TestEntity implements Shape {
    @Property
    private double radius;

    public Circle() {
    }

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double getArea() {
        return Math.PI * (radius * radius);
    }

    public double getRadius() {
        return radius;
    }
}
