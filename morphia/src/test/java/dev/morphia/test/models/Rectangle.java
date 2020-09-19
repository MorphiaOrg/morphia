package dev.morphia.test.models;

import dev.morphia.annotations.Property;

import static java.lang.String.format;

public class Rectangle extends TestEntity {
    @Property("h")
    private double height;
    @Property("w")
    private double width;

    public Rectangle() {
    }

    public Rectangle(double height, double width) {
        this.height = height;
        this.width = width;
    }

    public double getArea() {
        return height * width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getHeight());
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getWidth());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rectangle)) {
            return false;
        }

        final Rectangle rectangle = (Rectangle) o;

        if (Double.compare(rectangle.getHeight(), getHeight()) != 0) {
            return false;
        }
        return Double.compare(rectangle.getWidth(), getWidth()) == 0;

    }

    @Override
    public String toString() {
        return format("Rectangle{height=%s, width=%s}", height, width);
    }
}
