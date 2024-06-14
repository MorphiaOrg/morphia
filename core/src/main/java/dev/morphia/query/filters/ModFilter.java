package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ModFilter extends Filter {
    private final Number divisor;

    private final Number remainder;

    public ModFilter(String field, Number divisor, Number remainder) {
        super("$mod", field, null);
        this.divisor = divisor;
        this.remainder = remainder;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the divisor
     */
    @MorphiaInternal
    public Number divisor() {
        return divisor;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the remainder
     */
    @MorphiaInternal
    public Number remainder() {
        return remainder;
    }
}
