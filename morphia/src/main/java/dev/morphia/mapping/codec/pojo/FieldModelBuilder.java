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

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertySerialization;
import org.bson.codecs.pojo.TypeData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.bson.assertions.Assertions.notNull;

/**
 * A builder for programmatically creating {@code FieldModels}.
 *
 * @param <T> the type of the field
 * @morphia.internal
 */
public final class FieldModelBuilder<T> {
    private Field field;
    private String name;
    private String mappedName;
    private List<String> alternateNames = new ArrayList<>();
    private TypeData<T> typeData;
    private Codec<T> codec;
    private List<Annotation> annotations = emptyList();
    private PropertySerialization<T> serialization;
    private PropertyAccessor<T> accessor;
    private Boolean discriminatorEnabled;

    FieldModelBuilder() {
    }

    public PropertyAccessor<T> accessor() {
        return accessor;
    }

    /**
     * Sets the {@link PropertyAccessor}
     *
     * @param accessor the accessor
     * @return this
     */
    public FieldModelBuilder<T> accessor(final PropertyAccessor<T> accessor) {
        this.accessor = accessor;
        return this;
    }

    public List<String> alternateNames() {
        return alternateNames;
    }

    public void alternateName(final String name) {
        alternateNames.add(name);
    }

    /**
     * Sets the annotations
     *
     * @param annotations the annotations
     * @return this
     */
    public FieldModelBuilder<T> annotations(final List<Annotation> annotations) {
        this.annotations = unmodifiableList(notNull("annotations", annotations));
        return this;
    }

    /**
     * Creates the {@link FieldModel}.
     *
     * @return the FieldModel
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public FieldModel<T> build() {
        return new FieldModel(field, name, mappedName, typeData, annotations, codec, accessor, serialization);
    }

    /**
     * Sets a custom codec for the field
     *
     * @param codec the custom codec for the field
     * @return this
     */
    public FieldModelBuilder<T> codec(final Codec<T> codec) {
        this.codec = codec;
        return this;
    }

    public FieldModelBuilder<T> discriminatorEnabled(final Boolean discriminatorEnabled) {
        this.discriminatorEnabled = discriminatorEnabled;
        return this;
    }

    /**
     * Sets the field used
     *
     * @param field the field
     * @return this
     */
    public FieldModelBuilder<T> field(final Field field) {
        this.field = notNull("field", field);
        return this;
    }

    /**
     * Sets the field name
     *
     * @param fieldName the name
     * @return this
     */
    public FieldModelBuilder<T> fieldName(final String fieldName) {
        this.name = notNull("fieldName", fieldName);
        return this;
    }

    /**
     * Gets the annotation of this type.
     *
     * @param type the annotation class
     * @param <A>  the annotation type
     * @return the annotation instance or null if this annotation is on the field
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
     * Returns the read annotations,  to be applied when serializing to BSON
     *
     * @return the read annotations
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public Boolean getDiscriminatorEnabled() {
        return discriminatorEnabled;
    }

    /**
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type data
     */
    public TypeData<T> getTypeData() {
        return typeData;
    }

    /**
     * Checks this field for an annotation of the given type
     *
     * @param type the annotation class
     * @return true if the annotation is used on this field
     */
    public boolean hasAnnotation(final Class<? extends Annotation> type) {
        for (Annotation annotation : annotations) {
            if (type.equals(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the field's mapped name
     *
     * @param mappedName the name
     * @return this
     */
    public FieldModelBuilder<T> mappedName(final String mappedName) {
        this.mappedName = mappedName;
        return this;
    }

    /**
     * @return the field's mapped name
     */
    public String mappedName() {
        return this.mappedName;
    }

    /**
     * Sets the {@link PropertySerialization} checker
     *
     * @param propertySerialization checks if a property should be serialized
     * @return this
     */
    public FieldModelBuilder<T> serialization(final PropertySerialization<T> propertySerialization) {
        this.serialization = notNull("propertySerialization", propertySerialization);
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FieldModelBuilder.class.getSimpleName() + "[", "]")
                   .add("name='" + name + "'")
                   .add("mappedName='" + mappedName + "'")
                   .add("typeData=" + typeData)
                   .add("annotations=" + annotations)
                   .toString();
    }

    /**
     * Sets the type data
     *
     * @param typeData the type data
     * @return this
     */
    public FieldModelBuilder<T> typeData(final TypeData<T> typeData) {
        this.typeData = notNull("typeData", typeData);
        return this;
    }
}
