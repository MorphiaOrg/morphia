package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

@Entity("shapes")
public interface Shape {
    double getArea();
}
