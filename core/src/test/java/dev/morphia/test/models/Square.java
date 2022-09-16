package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import java.util.Objects;

@Entity
public class Square implements Shape {
    private double side;

    @Override
    public double getArea() {
        return side * side;
    }

    public double getSide() {
        return side;
    }

    public void setSide(double side) {
        this.side = side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(side);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Square)) {
            return false;
        }
        Square square = (Square) o;
        return Double.compare(square.side, side) == 0;
    }

    @Override
    public String toString() {
        return "Square{" +
                "side=" + side +
                '}';
    }
}
