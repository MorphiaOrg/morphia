package dev.morphia.query.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

import static java.lang.String.format;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class LogicalFilter extends Filter {
    private final List<Filter> filters;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    LogicalFilter(String name, Filter... filters) {
        super(name);
        this.filters = new ArrayList<>(Arrays.asList(filters));
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public Filter entityType(Class<?> type) {
        super.entityType(type);
        for (Filter filter : filters) {
            filter.entityType(type);
        }
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public Filter isValidating(boolean validate) {
        super.isValidating(validate);
        for (Filter filter : filters) {
            filter.isValidating(validate);
        }
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the filters
     */
    @MorphiaInternal
    public List<Filter> filters() {
        return filters;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Override
    @MorphiaInternal
    public String toString() {
        return format("%s: %s", getName(), filters);
    }
}
