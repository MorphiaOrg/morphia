package dev.morphia.benchmarks.models;

import dev.morphia.annotations.Entity;

@Entity
public class Circle extends Shape {
    private double radius;

    public Circle() {
    }

    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }
}
