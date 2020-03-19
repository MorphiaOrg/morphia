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

import dev.morphia.sofia.Sofia;
import morphia.org.bson.codecs.pojo.TypeData;
import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertySerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a field on a class and stores various metadata such as generic parameters.
 *
 * @param <T> the type of the field that the FieldModel represents.
 * @morphia.internal
 * @since 2.0
 */
public final class FieldModel<T> {
    private final Field field;
    private final String name;
    private final TypeData<T> typeData;
    private final String mappedName;
    private final Codec<T> codec;
    private final PropertyAccessor<T> accessor;
    private final PropertySerialization<T> serialization;
    private final List<Annotation> annotations;
    private volatile Codec<T> cachedCodec;
    private Class<?> normalizedType;

    FieldModel(final Field field, final String name, final String mappedName, final TypeData<T> typeData,
               final List<Annotation> annotations, final Codec<T> codec, final PropertyAccessor<T> accessor,
               final PropertySerialization<T> serialization) {
        this.field = Objects.requireNonNull(field, Sofia.notNull("field"));
        this.name = Objects.requireNonNull(name, Sofia.notNull("name"));
        this.mappedName = Objects.requireNonNull(mappedName, Sofia.notNull("name"));
        this.typeData = Objects.requireNonNull(typeData, Sofia.notNull("typeData"));
        this.annotations = annotations;
        this.codec = codec;
        this.cachedCodec = codec;
        this.accessor = accessor;
        this.serialization = serialization;

        this.field.setAccessible(true);
    }

    /**
     * Create a new {@link FieldModelBuilder}
     *
     * @param <T> the type of the field
     * @return the builder
     */
    public static <T> FieldModelBuilder<T> builder() {
        return new FieldModelBuilder<>();
    }

    /**
     * Gets the parameterized type of a TypeData
     *
     * @param toNormalize the type to normalize
     * @return the unwrapped type
     * @morphia.internal
     */
    public static Class<?> normalize(final TypeData<?> toNormalize) {
        Class<?> type;
        TypeData<?> typeData = toNormalize;
        while (!typeData.getTypeParameters().isEmpty()) {
            List<TypeData<?>> typeParameters = typeData.getTypeParameters();
            typeData = typeParameters.get(typeParameters.size() - 1);
        }
        type = typeData.getType();
        while (type.isArray()) {
            type = type.getComponentType();
        }
        return type;
    }

    /**
     * @return the accessor to use when accessing this field
     */
    public PropertyAccessor<T> getAccessor() {
        return accessor;
    }

    /**
     * Find an annotation of a specific type or null if not found.
     *
     * @param type the annotation type to find
     * @param <A>  the class type
     * @return the annotation instance or null
     */
    public <A extends Annotation> A getAnnotation(final Class<A> type) {
        for (Annotation annotation : annotations) {
            if (type.equals(annotation.annotationType())) {
                return type.cast(annotation);
            }
        }
        return null;
    }

    /**
     * @return the annotations on this Field
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * @return the cached codec
     */
    public Codec<T> getCachedCodec() {
        return cachedCodec;
    }

    /**
     * @return the custom codec to use if set or null
     */
    public Codec<T> getCodec() {
        return codec;
    }

    /**
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * @return the mapped name for the model
     */
    public String getMappedName() {
        return mappedName;
    }

    /**
     * @return the field name for the model
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the parameterized type of a List or the key type of a Map, e.g.
     *
     * @return the unwrapped type
     */
    public Class<?> getNormalizedType() {
        if (normalizedType == null) {
            normalizedType = normalize(getTypeData());
        }

        return normalizedType;
    }

    /**
     * @return the type data for the field
     */
    public TypeData<T> getTypeData() {
        return typeData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getField(), getName(), getTypeData(), getMappedName(), getCodec(), getAccessor(), serialization,
            getAnnotations(), getCachedCodec(), getNormalizedType());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldModel)) {
            return false;
        }
        final FieldModel<?> that = (FieldModel<?>) o;
        return getField().equals(that.getField())
               && getName().equals(that.getName())
               && getTypeData().equals(that.getTypeData())
               && getMappedName().equals(that.getMappedName())
               && Objects.equals(getCodec(), that.getCodec())
               && getAccessor().equals(that.getAccessor())
               && serialization.equals(that.serialization)
               && getAnnotations().equals(that.getAnnotations())
               && Objects.equals(getCachedCodec(), that.getCachedCodec())
               && Objects.equals(getNormalizedType(), that.getNormalizedType());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FieldModel.class.getSimpleName() + "[", "]")
                   .add("name='" + name + "'")
                   .add("mappedName='" + mappedName + "'")
                   .add("typeData=" + typeData)
                   .add("annotations=" + annotations)
                   .toString();
    }

    /**
     * Checks a value against the configured rules for serialization
     *
     * @param value the value to check
     * @return true if the given value should be serialized
     */
    public boolean shouldSerialize(final T value) {
        return serialization.shouldSerialize(value);
    }

    void cachedCodec(final Codec<T> codec) {
        this.cachedCodec = codec;
    }

}
