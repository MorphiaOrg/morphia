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
 * @morphia.internal
 * @since 2.0
 */
public final class FieldModelBuilder {
    private Field field;
    private String name;
    private String mappedName;
    private final List<String> alternateNames = new ArrayList<>();
    private TypeData<?> typeData;
    private Codec<? super Object> codec;
    private List<Annotation> annotations = emptyList();
    private PropertySerialization<? super Object> serialization;
    private PropertyAccessor<? super Object> accessor;
    private Boolean discriminatorEnabled;
    private EntityModel entityModel;

    FieldModelBuilder() {
    }

    /**
     * @return the accessor for this model
     */
    public PropertyAccessor<? super Object> accessor() {
        return accessor;
    }

    /**
     * Sets the {@link PropertyAccessor}
     *
     * @param accessor the accessor
     * @return this
     */
    public FieldModelBuilder accessor(PropertyAccessor<? super Object> accessor) {
        this.accessor = accessor;
        return this;
    }

    /**
     * Adds an alternate name
     *
     * @param name the new name
     */
    public void alternateName(String name) {
        alternateNames.add(name);
    }

    /**
     * @return the list of alternate names this model can be known by
     * @see dev.morphia.annotations.AlsoLoad
     */
    public List<String> alternateNames() {
        return alternateNames;
    }

    /**
     * Sets the annotations
     *
     * @param annotations the annotations
     * @return this
     */
    public FieldModelBuilder annotations(List<Annotation> annotations) {
        this.annotations = unmodifiableList(notNull("annotations", annotations));
        return this;
    }

    /**
     * Creates the {@link FieldModel}.
     *
     * @return the FieldModel
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public FieldModel build() {
        return new FieldModel(this);
    }

    /**
     * Sets a custom codec for the field
     *
     * @param codec the custom codec for the field
     * @return this
     */
    public FieldModelBuilder codec(Codec<? super Object> codec) {
        this.codec = codec;
        return this;
    }

    /**
     * @return the code
     * @since 2.1
     */
    public Codec<? super Object> codec() {
        return codec;
    }

    /**
     * Enables/disables the use of the discriminator during mapping
     *
     * @param discriminatorEnabled true if the discriminator should be used
     * @return this
     */
    public FieldModelBuilder discriminatorEnabled(Boolean discriminatorEnabled) {
        this.discriminatorEnabled = discriminatorEnabled;
        return this;
    }

    /**
     * Sets the entity model owner
     * @param entityModel the entity model
     * @return this
     * @since 2.1
     */
    public FieldModelBuilder entityModel(EntityModel entityModel) {
        this.entityModel = entityModel;
        return this;
    }

    /**
     * @return the entity model owner
     * @since 2.1
     */
    public EntityModel entityModel() {
        return entityModel;
    }

    /**
     * Sets the field used
     *
     * @param field the field
     * @return this
     */
    public FieldModelBuilder field(Field field) {
        this.field = notNull("field", field);
        return this;
    }

    /**
     * Sets the field name
     *
     * @param fieldName the name
     * @return this
     */
    public FieldModelBuilder fieldName(String fieldName) {
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
    public <A extends Annotation> A getAnnotation(Class<A> type) {
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
    public List<Annotation> annotations() {
        return annotations;
    }

    /**
     * @return true if the discriminator is to be used
     */
    public Boolean discriminatorEnabled() {
        return discriminatorEnabled;
    }

    /**
     * @return the field
     */
    public Field field() {
        return field;
    }

    /**
     * @return the field name
     */
    public String name() {
        return name;
    }

    /**
     * @return the type data
     */
    public TypeData<?> typeData() {
        return typeData;
    }

    /**
     * Checks this field for an annotation of the given type
     *
     * @param type the annotation class
     * @return true if the annotation is used on this field
     */
    public boolean hasAnnotation(Class<? extends Annotation> type) {
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
    public FieldModelBuilder mappedName(String mappedName) {
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
    public FieldModelBuilder serialization(PropertySerialization<? super Object> propertySerialization) {
        this.serialization = notNull("propertySerialization", propertySerialization);
        return this;
    }

    /**
     * @return the PropertySerialization for this property
     */
    public PropertySerialization<? super Object> serialization() {
        return serialization;
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
    public FieldModelBuilder typeData(TypeData<?> typeData) {
        this.typeData = notNull("typeData", typeData);
        return this;
    }
}
