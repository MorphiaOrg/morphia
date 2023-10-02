package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

public class RangeExpression extends Expression {
    private final int start;
    private final int end;
    private Integer step;

    public RangeExpression(int start, int end) {
        super("$range");
        this.start = start;
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Nullable
    public Integer step() {
        return step;
    }

    public RangeExpression step(Integer step) {
        this.step = step;
        return this;
    }
}
