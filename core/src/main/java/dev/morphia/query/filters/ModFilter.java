package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ModFilter extends Filter {
    private final long divisor;

    private final long remainder;

    public ModFilter(String field, long divisor, long remainder) {
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
    public long divisor() {
        return divisor;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the remainder
     */
    @MorphiaInternal
    public long remainder() {
        return remainder;
    }
}
