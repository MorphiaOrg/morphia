package dev.morphia.mapping.codec;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.ElemMatchFilter;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class ElemMatchFilterCodec extends BaseFilterCodec<ElemMatchFilter> {
    public ElemMatchFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ElemMatchFilter elemMatch, EncoderContext encoderContext) {
        document(writer, elemMatch.path(datastore.getMapper()), () -> {
            if (elemMatch.isNot()) {
                document(writer, "$not", () -> encodeFilter(writer, elemMatch, encoderContext));
            } else {
                encodeFilter(writer, elemMatch, encoderContext);
            }
        });

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void encodeFilter(BsonWriter writer, ElemMatchFilter elemMatch, EncoderContext encoderContext) {
        document(writer, elemMatch.getName(), () -> {
            List<Filter> filters = (List<Filter>) elemMatch.getValue();
            if (filters != null) {
                CodecRegistry registry = datastore.getCodecRegistry();
                for (Filter filter : filters) {
                    Codec codec = registry.get(filter.getClass());
                    codec.encode(writer, filter, encoderContext);
                }
            }
        });
    }

    @Override
    public Class<ElemMatchFilter> getEncoderClass() {
        return ElemMatchFilter.class;
    }
}
