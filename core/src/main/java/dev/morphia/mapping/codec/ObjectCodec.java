package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
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
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Defines a generic codec for Objects that will attempt to discover and use the correct codec.
 */
public class ObjectCodec implements Codec<Object> {

    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private MorphiaDatastore datastore;
    // whether a discriminator under an entity's own key makes a document that entity, or only one under the configured key
    private final boolean ownKeys;
    private final ObjectCodec forIds;

    /**
     * Creates a codec
     *
     */
    public ObjectCodec(MorphiaDatastore datastore) {
        this(datastore, true);
    }

    private ObjectCodec(MorphiaDatastore datastore, boolean ownKeys) {
        this.datastore = datastore;
        this.ownKeys = ownKeys;
        forIds = ownKeys ? new ObjectCodec(datastore, false) : this;
    }

    /**
     * Ids stored as documents are read with this, by the decode session for its keys and by references for the ids they
     * look up, so that both sides agree. Only a discriminator under the configured key makes one an entity: one under an
     * entity's own key leaves it a {@code Document}.
     *
     * @param registry the registry to find the {@code Object} codec in
     * @return the codec to read ids with
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static Codec<Object> forIds(CodecRegistry registry) {
        Codec<Object> codec = registry.get(Object.class);
        return codec instanceof ObjectCodec objectCodec ? objectCodec.forIds : codec;
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
                        || ownKeys && reader.getCurrentBsonType() == BsonType.STRING && lookup.isKey(configuredKey, name)) {
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
