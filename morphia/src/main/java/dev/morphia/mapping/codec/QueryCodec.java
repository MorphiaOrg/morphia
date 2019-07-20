package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import dev.morphia.query.QueryImpl;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class QueryCodec implements Codec<QueryImpl> {
    private Mapper mapper;

    public QueryCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public QueryImpl decode(final BsonReader reader, final DecoderContext decoderContext) {
        return null;
    }

    @Override
    public void encode(final BsonWriter writer, final QueryImpl value, final EncoderContext encoderContext) {
        mapper.getCodecRegistry().get(Document.class).encode(writer, value.getQueryDocument(), encoderContext);
    }

    @Override
    public Class<QueryImpl> getEncoderClass() {
        return QueryImpl.class;
    }
}
