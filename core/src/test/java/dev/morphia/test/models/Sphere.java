package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

@Entity
public class Sphere extends Circle {
    public Sphere() {
    }

    public Sphere(double radius) {
        super(radius);
    }
}
