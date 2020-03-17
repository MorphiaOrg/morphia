package dev.morphia.query.experimental.filters;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

class ElemMatchFilter extends Filter {
    ElemMatchFilter(final String field, final List<Filter> query) {
        super("$elemMatch", field, query);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getFilterName());
        List<Filter> filters = (List<Filter>) getValue();
        for (final Filter filter : filters) {
            filter.encode(mapper, writer, context);
        }
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

}
