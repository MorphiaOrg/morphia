/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.PropertyHandler;
import org.bson.BsonDocumentReader;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static dev.morphia.mapping.codec.Conversions.convert;


public class MorphiaCodec<T> extends BaseMorphiaCodec<T> implements CollectibleCodec<T> {
    private Mapper mapper;
    private MappedClass mappedClass;

    MorphiaCodec(final Mapper mapper, final MappedClass mappedClass, final ClassModel<T> classModel,
                 final CodecRegistry registry, final List<PropertyCodecProvider> propertyCodecProviders,
                 final DiscriminatorLookup discriminatorLookup) {
        super(classModel, registry, propertyCodecProviders, discriminatorLookup);
        this.mapper = mapper;
        this.mappedClass = mappedClass;
    }

    private MorphiaCodec(final Mapper mapper, final MappedClass mappedClass, final ClassModel<T> classModel,
                 final CodecRegistry registry, final PropertyCodecRegistry propertyCodecRegistry,
                 final DiscriminatorLookup discriminatorLookup, final boolean specialized) {
        super(classModel, registry, propertyCodecRegistry, discriminatorLookup, new ConcurrentHashMap<>(), specialized);
        this.mapper = mapper;
        this.mappedClass = mappedClass;
    }

    @Override
    public T generateIdIfAbsentFromDocument(final T document) {
        if (!documentHasId(document)) {
            final MappedField mappedIdField = mappedClass.getMappedIdField();
            mappedIdField.setFieldValue(document, convert(new ObjectId(), mappedIdField.getType()));
        }
        return document;
    }

    @Override
    public boolean documentHasId(final T document) {
        return mappedClass.getMappedIdField().getFieldValue(document) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BsonValue getDocumentId(final T document) {
        final Object id = mappedClass.getMappedIdField().getFieldValue(document);
        final DocumentWriter writer = new DocumentWriter();
        ((Codec) getRegistry().get(id.getClass()))
            .encode(writer, id, EncoderContext.builder().build());
        return writer.getRoot();
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        if (mappedClass.hasLifecycle(PostPersist.class)
            || mappedClass.hasLifecycle(PrePersist.class)
            || mapper.hasInterceptors()) {
            final DocumentWriter documentWriter = new DocumentWriter();
            super.encode(documentWriter, value, encoderContext);
            Document document = documentWriter.getRoot();

            mappedClass.callLifecycleMethods(PrePersist.class, value, document, mapper);

            getRegistry().get(Document.class).encode(writer, document, encoderContext);

            mappedClass.callLifecycleMethods(PostPersist.class, value, document, mapper);

        } else {
            super.encode(writer, value, encoderContext);
        }
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        T entity;
        if (mappedClass.hasLifecycle(PreLoad.class) || mappedClass.hasLifecycle(PostLoad.class) || mapper.hasInterceptors()) {
            final InstanceCreator<T> instanceCreator = getClassModel().getInstanceCreator();
            entity = instanceCreator.getInstance();

            Document document = getRegistry().get(Document.class).decode(reader, decoderContext);
            mappedClass.callLifecycleMethods(PreLoad.class, entity, document, mapper);

            decodeProperties(new BsonDocumentReader(document.toBsonDocument(Document.class, mapper.getCodecRegistry())), decoderContext,
                instanceCreator);

            mappedClass.callLifecycleMethods(PostLoad.class, entity, document, mapper);
        } else {
            entity = super.decode(reader, decoderContext);
        }

        return entity;
    }

    @Override
    protected <S> void encodeProperty(final BsonWriter writer,
                                      final T instance,
                                      final EncoderContext encoderContext,
                                      final PropertyModel<S> propertyModel) {
        final PropertyHandler handler = getPropertyHandler(getClassModel().getInstanceCreator(), propertyModel);
        if (handler != null) {
            handler.encodeProperty(writer, instance, encoderContext, propertyModel);
        } else {
            super.encodeProperty(writer, instance, encoderContext, propertyModel);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S> void decodePropertyModel(final BsonReader reader,
                                           final DecoderContext decoderContext,
                                           final InstanceCreator<T> instanceCreator,
                                           final String name,
                                           final PropertyModel<S> propertyModel) {
        if (propertyModel != null) {
            final PropertyHandler handler = getPropertyHandler(instanceCreator, propertyModel);
            if (handler != null) {
                S value = handler.decodeProperty(reader, decoderContext, instanceCreator, name, propertyModel);
                instanceCreator.set(value, propertyModel);
            } else {
                final BsonReaderMark mark = reader.getMark();
                try {
                    super.decodePropertyModel(reader, decoderContext, instanceCreator, name, propertyModel);
                } catch (CodecConfigurationException e) {
                    mark.reset();
                    final Object value = mapper.getCodecRegistry().get(Object.class).decode(reader, decoderContext);
                    instanceCreator.set((S) convert(value, propertyModel.getTypeData().getType()), propertyModel);
                }
            }
        } else {
            reader.skipValue();
        }
    }

    private <S> PropertyHandler getPropertyHandler(final InstanceCreator<?> instanceCreator, final PropertyModel<S> propertyModel) {
        return instanceCreator instanceof MorphiaInstanceCreator ? ((MorphiaInstanceCreator) instanceCreator).getHandler(propertyModel)
                                                                 : null;

    }

    private <S> MappedField getMappedField(final PropertyModel<S> propertyModel) {
        final MappedField field = mappedClass.getMappedField(propertyModel.getName());
        return field != null ?  field : mappedClass.getMappedFieldByJavaField(propertyModel.getName());
    }

    public MappedClass getMappedClass() {
        return mappedClass;
    }

    @Override
    protected <S> PojoCodec<S> getSpecializedCodec(final ClassModel<S> specialized) {
        return new SpecializedMorphiaCodec(this, specialized);
    }

    private static class SpecializedMorphiaCodec<T> extends PojoCodec<T> {

        private final MorphiaCodec morphiaCodec;
        private final ClassModel<T> classModel;
        private PojoCodec<T> specialized;

        SpecializedMorphiaCodec(final MorphiaCodec morphiaCodec, final ClassModel<T> classModel) {
            this.morphiaCodec = morphiaCodec;
            this.classModel = classModel;
        }

        @Override
        public ClassModel<T> getClassModel() {
            return classModel;
        }

        @Override
        public T decode(final BsonReader reader, final DecoderContext decoderContext) {
            return getSpecialized().decode(reader, decoderContext);
        }

        private PojoCodec<T> getSpecialized() {
            if (specialized == null) {
                specialized = new MorphiaCodec<>(morphiaCodec.mapper, morphiaCodec.mappedClass, classModel, morphiaCodec.getRegistry(),
                    morphiaCodec.getPropertyCodecRegistry(),
                    morphiaCodec.getDiscriminatorLookup(), true);
            }
            return specialized;
        }

        @Override
        public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
            getSpecialized().encode(writer, value, encoderContext);
        }

        @Override
        public Class<T> getEncoderClass() {
            return classModel.getType();
        }
    }
}
