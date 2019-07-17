package dev.morphia.testmodel;

import dev.morphia.annotations.Entity;

@Entity("shapes")
public interface Shape {
    double getArea();
}
