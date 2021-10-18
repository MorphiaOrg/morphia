package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappingException;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

/**
 * Defines a generic codec for Objects that will attempt to discover and use the correct codec.
 */
public class ObjectCodec implements Codec<Object> {

    private final Datastore datastore;
    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();

    /**
     * Creates a codec
     *
     * @param datastore the Datastore to use
     */
    public ObjectCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        BsonType bsonType = reader.getCurrentBsonType();
        Class<?> clazz;
        if (bsonType == BsonType.DOCUMENT) {
            clazz = Document.class;
            String discriminatorField = datastore.getMapper().getOptions().getDiscriminatorKey();

            BsonReaderMark mark = reader.getMark();
            reader.readStartDocument();
            while (clazz.equals(Document.class) && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (reader.readName().equals(discriminatorField)) {
                    try {
                        clazz = datastore.getMapper().getClass(reader.readString());
                    } catch (CodecConfigurationException e) {
                        throw new MappingException(e.getMessage(), e);
                    }
                } else {
                    reader.skipValue();
                }
            }
            mark.reset();
        } else {
            clazz = bsonTypeClassMap.get(bsonType);
        }
        return datastore.getCodecRegistry()
                        .get(clazz)
                        .decode(reader, decoderContext);
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        final Codec codec = datastore.getCodecRegistry().get(value.getClass());
        codec.encode(writer, value, encoderContext);
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
    }
}
