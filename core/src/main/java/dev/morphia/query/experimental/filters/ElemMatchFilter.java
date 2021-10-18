package dev.morphia.query.experimental.filters;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

class ElemMatchFilter extends Filter {
    ElemMatchFilter(String field, List<Filter> query) {
        super("$elemMatch", field, query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
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
