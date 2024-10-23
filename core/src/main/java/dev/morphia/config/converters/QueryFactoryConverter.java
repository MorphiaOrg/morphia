package dev.morphia.config.converters;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.QueryFactory;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class QueryFactoryConverter extends ClassNameConverter<QueryFactory> {
    @Override
    public QueryFactory convert(String value) {
        return (QueryFactory) super.convert(value);
    }
}
