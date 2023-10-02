package dev.morphia.aggregation.expressions.impls;

public class SliceExpression extends Expression {
    private final Expression array;
    private final int size;
    private Integer position;

    public SliceExpression(Expression array, int size) {
        super("$slice");
        this.array = array;
        this.size = size;
    }

    public Expression array() {
        return array;
    }

    public int size() {
        return size;
    }

    public Integer position() {
        return position;
    }

    public SliceExpression position(Integer position) {
        this.position = position;
        return this;
    }
}
