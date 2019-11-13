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
import org.bson.codecs.Codec;
import org.bson.codecs.pojo.TypeData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a field on a class and stores various metadata such as generic parameters
 *
 * @param <T> the type of the field that the FieldModel represents.
 * @since 3.5
 */
public final class FieldModel<T> {
    private final Field field;
    private final String name;
    private final TypeData<T> typeData;
    private final String mappedName;
    private List<Annotation> annotations;
    private final Codec<T> codec;
    private volatile Codec<T> cachedCodec;

    FieldModel(final Field field, final String name, final String mappedName, final TypeData<T> typeData,
               final List<Annotation> annotations, final Codec<T> codec) {
        this.field = Objects.requireNonNull(field, Sofia.notNull("field"));
        this.name = Objects.requireNonNull(name, Sofia.notNull("name"));
        this.mappedName = Objects.requireNonNull(mappedName, Sofia.notNull("name"));
        this.typeData = Objects.requireNonNull(typeData, Sofia.notNull("typeData"));
        this.annotations = annotations;
        this.codec = codec;
        this.cachedCodec = codec;

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
     * @return the field name for the model
     */
    public String getName() {
        return name;
    }

    /**
     * @return the mapped name for the model
     */
    public String getMappedName() {
        return mappedName;
    }

    /**
     * @return the type data for the field
     */
    public TypeData<T> getTypeData() {
        return typeData;
    }

    /**
     * @return the annotations on this Field
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Find an annotation of a specific type or null if not found.
     *
     * @param type the annotation type to find
     * @param <A>   the class type
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
     * @return the custom codec to use if set or null
     */
    public Codec<T> getCodec() {
        return codec;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldModel)) {
            return false;
        }

        final FieldModel<?> that = (FieldModel<?>) o;

        if (!field.equals(that.field)) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        if (!typeData.equals(that.typeData)) {
            return false;
        }
        if (codec != null ? !codec.equals(that.codec) : that.codec != null) {
            return false;
        }
        return cachedCodec != null ? cachedCodec.equals(that.cachedCodec) : that.cachedCodec == null;
    }

    @Override
    public int hashCode() {
        int result = field.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + typeData.hashCode();
        result = 31 * result + (codec != null ? codec.hashCode() : 0);
        result = 31 * result + (cachedCodec != null ? cachedCodec.hashCode() : 0);
        return result;
    }

    /**
     * @return the field
     */
    public Field getField() {
        return field;
    }
}
