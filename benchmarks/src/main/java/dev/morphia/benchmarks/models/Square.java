package dev.morphia.benchmarks.models;

import dev.morphia.annotations.Entity;

@Entity
public class Square extends Shape {
    private double side;

    public Square() {
    }

    public Square(String color, double side) {
        super(color);
        this.side = side;
    }

    public double getSide() {
        return side;
    }
}
