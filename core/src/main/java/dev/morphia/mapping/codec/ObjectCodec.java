package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.DiscriminatorLookup;
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

    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private MorphiaDatastore datastore;

    /**
     * Creates a codec
     *
     */
    public ObjectCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        BsonType bsonType = reader.getCurrentBsonType();
        Class<?> clazz;
        if (bsonType == BsonType.DOCUMENT) {
            clazz = Document.class;
            String configuredKey = datastore.getMapper().getConfig().discriminatorKey();
            DiscriminatorLookup lookup = datastore.getMapper().getDiscriminatorLookup();

            BsonReaderMark mark = reader.getMark();
            reader.readStartDocument();
            while (clazz.equals(Document.class) && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                // under the configured key a value that isn't a string fails, as it always has; under an entity's own
                // key, it's just a field
                if (name.equals(configuredKey)
                        || reader.getCurrentBsonType() == BsonType.STRING && lookup.isKey(configuredKey, name)) {
                    try {
                        Class<?> entity = lookup.lookup(configuredKey, name, reader.readString());
                        if (entity != null) {
                            clazz = entity;
                        }
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
