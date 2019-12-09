package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
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
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PropertyModel;

import static dev.morphia.mapping.codec.Conversions.convert;

class Decoder<T> implements org.bson.codecs.Decoder<T> {
    private final MorphiaCodec<T> morphiaCodec;

    public Decoder(final MorphiaCodec<T> morphiaCodec) {
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
            EntityModel<T> classModel = morphiaCodec.getClassModel();
            if (decoderContext.hasCheckedDiscriminator()) {
                MorphiaInstanceCreator<T> instanceCreator = classModel.getInstanceCreator();
                decodeProperties(reader, decoderContext, instanceCreator);
                return instanceCreator.getInstance();
            } else {
                entity = getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(), morphiaCodec.getRegistry(),
                    morphiaCodec.getDiscriminatorLookup(), morphiaCodec)
                             .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
            }
        }

        return entity;
    }

    protected void decodeProperties(final BsonReader reader, final DecoderContext decoderContext,
                                    final MorphiaInstanceCreator<T> instanceCreator) {
        reader.readStartDocument();
        EntityModel<T> classModel = morphiaCodec.getClassModel();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodePropertyModel(reader, decoderContext, instanceCreator, classModel.getPropertyModel(name));
            }
        }
        reader.readEndDocument();
    }

    @SuppressWarnings("unchecked")
    protected <S> void decodePropertyModel(final BsonReader reader, final DecoderContext decoderContext,
                                           final InstanceCreator<T> instanceCreator, final PropertyModel<S> propertyModel) {

        if (propertyModel != null) {
            final BsonReaderMark mark = reader.getMark();
            try {
                S value = null;
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    value = decoderContext.decodeWithChildContext(propertyModel.getCachedCodec(), reader);
                }
                instanceCreator.set(value, propertyModel);
            } catch (BsonInvalidOperationException e) {
                mark.reset();
                final Object value = morphiaCodec.getMapper().getCodecRegistry().get(Object.class).decode(reader, decoderContext);
                instanceCreator.set((S) convert(value, propertyModel.getTypeData().getType()), propertyModel);
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
                    morphiaCodec.getClassModel().getName(), e.getMessage()), e);
            } finally {
                mark.reset();
            }
        }
        return codec != null ? codec : defaultCodec;
    }

    private T decodeWithLifecycle(final BsonReader reader, final DecoderContext decoderContext) {
        final T entity;
        final MorphiaInstanceCreator<T> instanceCreator = morphiaCodec.getClassModel().getInstanceCreator();
        entity = instanceCreator.getInstance();

        Document document = morphiaCodec.getRegistry().get(Document.class).decode(reader, decoderContext);
        morphiaCodec.getMappedClass().callLifecycleMethods(PreLoad.class, entity, document, morphiaCodec.getMapper());

        decodeProperties(new DocumentReader(document), decoderContext, instanceCreator);

        morphiaCodec.getMappedClass().callLifecycleMethods(PostLoad.class, entity, document, morphiaCodec.getMapper());
        return entity;
    }

}
