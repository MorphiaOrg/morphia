package dev.morphia.mapping.experimental;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

public class ListReferenceCodec implements Codec<ListReference> {
    private Mapper mapper;
    private BsonTypeClassMap typeMap;

    public ListReferenceCodec(final Mapper mapper, final BsonTypeClassMap typeMap) {
        this.mapper = mapper;
        this.typeMap = typeMap;
    }

    @Override
    public Class<ListReference> getEncoderClass() {
        return ListReference.class;
    }

    @SuppressWarnings("unchecked")
    public ListReference decode(final BsonReader reader, final DecoderContext decoderContext) {
        List decode = (List) mapper.getCodecRegistry()
                                   .get(typeMap.get(reader.getCurrentBsonType()))
                                   .decode(reader, decoderContext);
        return new ListReference(mapper.getDatastore(), decode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(final BsonWriter writer, final ListReference value, final EncoderContext encoderContext) {
        if (value == null) {
            writer.writeNull();
        } else {
            List ids = value.getIds();
            if(ids == null) {
                List values = value.getValues();
                final List newIds = new ArrayList();
                values.forEach(e -> newIds.add(mapper.getMappedClass(e.getClass())
                                                   .getIdField()
                                                   .getFieldValue(e)));
                ids = newIds;
            }


            final Codec codec = mapper.getCodecRegistry().get(ids.getClass());
            codec.encode(writer, ids, encoderContext);
        }

    }
}
