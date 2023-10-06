package dev.morphia.query.filters;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class ModFilter extends Filter {
    private final long divisor;

    private final long remainder;

    public ModFilter(String field, long divisor, long remainder) {
        super("$mod", field, null);
        this.divisor = divisor;
        this.remainder = remainder;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(datastore.getMapper()));
        writer.writeName(getName());
        writer.writeStartArray();
        writeValue(divisor, datastore, writer, context);
        writeValue(remainder, datastore, writer, context);
        writer.writeEndArray();
        writer.writeEndDocument();
    }
}
