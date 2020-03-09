package dev.morphia.query.experimental.filters;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

class LogicalFilter extends Filter {
    private final List<Filter> filters;

    LogicalFilter(final String name, final Filter... filters) {
        super(name);
        this.filters = Arrays.asList(filters);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartArray(getFilterName());
        for (final Filter filter : filters) {
            writer.writeStartDocument();
            filter.encode(mapper, writer, context);
            writer.writeEndDocument();
        }
        writer.writeEndArray();
    }

    @Override
    public Filter entityType(final Class<?> type) {
        super.entityType(type);
        for (final Filter filter : filters) {
            filter.entityType(type);
        }
        return this;
    }

    @Override
    public Filter isValidating(final boolean validate) {
        super.isValidating(validate);
        for (final Filter filter : filters) {
            filter.isValidating(validate);
        }
        return this;
    }

    @Override
    public String toString() {
        return format("%s: %s", getFilterName(), filters);
    }
}
