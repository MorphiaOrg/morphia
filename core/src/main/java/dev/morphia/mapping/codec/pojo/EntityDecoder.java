package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.DiscriminatorLookup;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.reader.DocumentReader;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.Conversions.convert;

/**
 * @morphia.internal
 * @since 2.0
 */
public class EntityDecoder implements org.bson.codecs.Decoder<Object> {
    private final MorphiaCodec<?> morphiaCodec;

    protected EntityDecoder(MorphiaCodec<?> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        Object entity;
        if (morphiaCodec.getMappedClass().hasLifecycle(PreLoad.class)
            || morphiaCodec.getMappedClass().hasLifecycle(PostLoad.class)
            || morphiaCodec.getMapper().hasInterceptors()) {
            entity = decodeWithLifecycle(reader, decoderContext);
        } else {
            EntityModel classModel = morphiaCodec.getEntityModel();
            if (decoderContext.hasCheckedDiscriminator()) {
                MorphiaInstanceCreator instanceCreator = getInstanceCreator(classModel);
                decodeProperties(reader, decoderContext, instanceCreator);
                return instanceCreator.getInstance();
            } else {
                entity = getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(),
                    morphiaCodec.getRegistry(), morphiaCodec.getDiscriminatorLookup(), morphiaCodec)
                             .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
            }
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    protected void decodeModel(BsonReader reader, DecoderContext decoderContext,
                               MorphiaInstanceCreator instanceCreator, FieldModel model) {

        if (model != null) {
            final BsonReaderMark mark = reader.getMark();
            try {
                Object value = null;
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    value = decoderContext.decodeWithChildContext(model.getCachedCodec(), reader);
                }
                instanceCreator.set(value, model);
            } catch (BsonInvalidOperationException e) {
                mark.reset();
                final Object value = morphiaCodec.getMapper().getCodecRegistry().get(Object.class).decode(reader, decoderContext);
                instanceCreator.set(convert(value, model.getTypeData().getType()), model);
            }
        } else {
            reader.skipValue();
        }
    }

    protected void decodeProperties(BsonReader reader, DecoderContext decoderContext,
                                    MorphiaInstanceCreator instanceCreator) {
        reader.readStartDocument();
        EntityModel classModel = morphiaCodec.getEntityModel();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodeModel(reader, decoderContext, instanceCreator, classModel.getFieldModelByName(name));
            }
        }
        reader.readEndDocument();
    }

    @SuppressWarnings("unchecked")
    protected Codec<?> getCodecFromDocument(BsonReader reader, boolean useDiscriminator, String discriminatorKey,
                                            CodecRegistry registry, DiscriminatorLookup discriminatorLookup,
                                            Codec<?> defaultCodec) {
        Codec<?> codec = null;
        if (useDiscriminator) {
            BsonReaderMark mark = reader.getMark();
            try {
                reader.readStartDocument();
                while (codec == null && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    if (discriminatorKey.equals(reader.readName())) {
                        codec = registry.get(discriminatorLookup.lookup(reader.readString()));
                    } else {
                        reader.skipValue();
                    }
                }
            } catch (Exception e) {
                throw new CodecConfigurationException(String.format("Failed to decode '%s'. Decoding errored with: %s",
                    morphiaCodec.getEntityModel().getName(), e.getMessage()), e);
            } finally {
                mark.reset();
            }
        }
        return codec != null ? codec : defaultCodec;
    }

    protected MorphiaInstanceCreator getInstanceCreator(EntityModel classModel) {
        return classModel.getInstanceCreator();
    }

    private Object decodeWithLifecycle(BsonReader reader, DecoderContext decoderContext) {
        final Object entity;
        final MorphiaInstanceCreator instanceCreator = getInstanceCreator(morphiaCodec.getEntityModel());
        entity = instanceCreator.getInstance();

        Document document = morphiaCodec.getRegistry().get(Document.class).decode(reader, decoderContext);
        morphiaCodec.getMappedClass().callLifecycleMethods(PreLoad.class, entity, document, morphiaCodec.getMapper());

        decodeProperties(new DocumentReader(document), decoderContext, instanceCreator);

        morphiaCodec.getMappedClass().callLifecycleMethods(PostLoad.class, entity, document, morphiaCodec.getMapper());
        return entity;
    }

}
