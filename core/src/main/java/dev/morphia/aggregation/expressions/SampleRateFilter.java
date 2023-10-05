package dev.morphia.aggregation.expressions;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.filters.Filter;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class SampleRateFilter extends Filter {
    public SampleRateFilter(double rate) {
        super("$sampleRate", null, rate);
    }
}
