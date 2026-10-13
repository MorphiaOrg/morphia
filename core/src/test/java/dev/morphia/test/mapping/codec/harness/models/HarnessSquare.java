package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

@Entity
public class HarnessSquare extends HarnessShape {
    private double side;

    public HarnessSquare() {
    }

    public HarnessSquare(String label, double side) {
        super(label);
        this.side = side;
    }
}
