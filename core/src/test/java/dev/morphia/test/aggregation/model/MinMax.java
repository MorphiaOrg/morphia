package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;

@Entity
public class MinMax {
    private int min;
    private int max;

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    @Override
    public String toString() {
        return "MinMax{"
               + "min=" + min
               + ", max=" + max
               + '}';
    }
}
