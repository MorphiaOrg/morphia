package dev.morphia.aggregation.experimental.stages;

public class Skip extends Stage {
    private int size;

    protected Skip(final int size) {
        super("$skip");
        this.size = size;
    }

    public static Skip of(final int i) {
        return new Skip(i);
    }

    public int getSize() {
        return size;
    }
}
