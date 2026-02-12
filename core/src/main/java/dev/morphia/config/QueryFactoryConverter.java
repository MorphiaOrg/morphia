package dev.morphia.config;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.QueryFactory;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class QueryFactoryConverter extends ClassNameConverter<QueryFactory> {
    public QueryFactoryConverter(MorphiaConfig morphiaConfig) {
        super(morphiaConfig);
    }

    @Override
    public QueryFactory convert(String value) {
        return (QueryFactory) super.convert(value);
    }
}
