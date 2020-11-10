package dev.morphia.query.experimental.filters;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

class ElemMatchFilter extends Filter {
    ElemMatchFilter(String field, List<Filter> query) {
        super("$elemMatch", field, query);
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(mapper));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getName());
        List<Filter> filters = (List<Filter>) getValue();
        for (Filter filter : filters) {
            filter.encode(mapper, writer, context);
        }
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

}
