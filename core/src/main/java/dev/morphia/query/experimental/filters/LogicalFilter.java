package dev.morphia.query.experimental.filters;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Arrays;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static java.lang.String.format;

class LogicalFilter extends Filter {
    private final List<Filter> filters;

    LogicalFilter(String name, Filter... filters) {
        super(name);
        this.filters = Arrays.asList(filters);
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
        writer.writeStartArray(getName());
        for (Filter filter : filters) {
            document(writer, () -> filter.encode(datastore, writer, context));
        }
        writer.writeEndArray();
    }

    @Override
    public Filter entityType(Class<?> type) {
        super.entityType(type);
        for (Filter filter : filters) {
            filter.entityType(type);
        }
        return this;
    }

    @Override
    public Filter isValidating(boolean validate) {
        super.isValidating(validate);
        for (Filter filter : filters) {
            filter.isValidating(validate);
        }
        return this;
    }

    @Override
    public String toString() {
        return format("%s: %s", getName(), filters);
    }
}
