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
public class EntityDecoder<T> implements org.bson.codecs.Decoder<T> {
    private final MorphiaCodec<T> morphiaCodec;

    protected EntityDecoder(final MorphiaCodec<T> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        T entity;
        if (morphiaCodec.getMappedClass().hasLifecycle(PreLoad.class)
            || morphiaCodec.getMappedClass().hasLifecycle(PostLoad.class)
            || morphiaCodec.getMapper().hasInterceptors()) {
            entity = decodeWithLifecycle(reader, decoderContext);
        } else {
            EntityModel<T> classModel = morphiaCodec.getEntityModel();
            if (decoderContext.hasCheckedDiscriminator()) {
                MorphiaInstanceCreator<T> instanceCreator = getInstanceCreator(classModel);
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

    protected MorphiaInstanceCreator<T> getInstanceCreator(final EntityModel<T> classModel) {
        return classModel.getInstanceCreator();
    }

    protected void decodeProperties(final BsonReader reader, final DecoderContext decoderContext,
                                    final MorphiaInstanceCreator<T> instanceCreator) {
        reader.readStartDocument();
        EntityModel<T> classModel = morphiaCodec.getEntityModel();
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
    protected <S> void decodeModel(final BsonReader reader, final DecoderContext decoderContext,
                                   final MorphiaInstanceCreator<T> instanceCreator, final FieldModel<S> model) {

        if (model != null) {
            final BsonReaderMark mark = reader.getMark();
            try {
                S value = null;
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    value = decoderContext.decodeWithChildContext(model.getCachedCodec(), reader);
                }
                instanceCreator.set(value, model);
            } catch (BsonInvalidOperationException e) {
                mark.reset();
                final Object value = morphiaCodec.getMapper().getCodecRegistry().get(Object.class).decode(reader, decoderContext);
                instanceCreator.set((S) convert(value, model.getTypeData().getType()), model);
            }
        } else {
            reader.skipValue();
        }
    }

    @SuppressWarnings("unchecked")
    protected Codec<T> getCodecFromDocument(final BsonReader reader, final boolean useDiscriminator, final String discriminatorKey,
                                            final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                            final Codec<T> defaultCodec) {
        Codec<T> codec = null;
        if (useDiscriminator) {
            BsonReaderMark mark = reader.getMark();
            try {
                reader.readStartDocument();
                while (codec == null && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    if (discriminatorKey.equals(reader.readName())) {
                        codec = (Codec<T>) registry.get(discriminatorLookup.lookup(reader.readString()));
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

    private T decodeWithLifecycle(final BsonReader reader, final DecoderContext decoderContext) {
        final T entity;
        final MorphiaInstanceCreator<T> instanceCreator = getInstanceCreator(morphiaCodec.getEntityModel());
        entity = instanceCreator.getInstance();

        Document document = morphiaCodec.getRegistry().get(Document.class).decode(reader, decoderContext);
        morphiaCodec.getMappedClass().callLifecycleMethods(PreLoad.class, entity, document, morphiaCodec.getMapper());

        decodeProperties(new DocumentReader(document), decoderContext, instanceCreator);

        morphiaCodec.getMappedClass().callLifecycleMethods(PostLoad.class, entity, document, morphiaCodec.getMapper());
        return entity;
    }

}
