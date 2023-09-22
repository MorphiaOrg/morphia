package dev.morphia.query.filters;

import java.util.List;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class ElemMatchFilter extends Filter {
    ElemMatchFilter(String field, List<Filter> query) {
        super("$elemMatch", field, query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(datastore.getMapper()));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getName());
        List<Filter> filters = (List<Filter>) getValue();
        if (filters != null) {
            for (Filter filter : filters) {
                filter.encode(datastore, writer, context);
            }
        }
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

}
