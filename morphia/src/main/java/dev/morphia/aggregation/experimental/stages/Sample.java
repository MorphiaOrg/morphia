package dev.morphia.aggregation.experimental.stages;

public class Sample extends Stage {
    private int size;

    protected Sample(final int size) {
        super("$sample");
        this.size = size;
    }

    public static Sample of(final int i) {
        return new Sample(1);
    }

    public int getSize() {
        return size;
    }
}
