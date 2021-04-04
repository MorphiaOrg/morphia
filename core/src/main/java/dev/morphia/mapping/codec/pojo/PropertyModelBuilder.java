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

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertySerialization;

import java.lang.annotation.Annotation;
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
public final class PropertyModelBuilder {
    private final Datastore datastore;
    private final List<String> alternateNames = new ArrayList<>();
    private PropertyAccessor<? super Object> accessor;
    private List<Annotation> annotations = emptyList();
    private Boolean discriminatorEnabled;
    private EntityModel owner;
    private String mappedName;
    private int modifiers;
    private String name;
    private TypeData<?> typeData;
    private MorphiaPropertySerialization serialization;

    PropertyModelBuilder(Datastore datastore) {
        this.datastore = datastore;
    }

    public PropertyModelBuilder discoverMappedName(MapperOptions options) {
        Property property = getAnnotation(Property.class);
        Reference reference = getAnnotation(Reference.class);
        Version version = getAnnotation(Version.class);

        if (hasAnnotation(Id.class)) {
            mappedName("_id");
        } else if (property != null && !property.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName(property.value());
        } else if (reference != null && !reference.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName(reference.value());
        } else if (version != null && !version.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName(version.value());
        } else {
            mappedName(options.getFieldNaming().apply(name()));
        }
        return this;
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
    public PropertyModelBuilder accessor(PropertyAccessor<? super Object> accessor) {
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
    public PropertyModelBuilder annotations(List<Annotation> annotations) {
        this.annotations = unmodifiableList(notNull("annotations", annotations));
        return this;
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
     * Creates the {@link PropertyModel}.
     *
     * @return the FieldModel
     */
    public PropertyModel build() {
        return new PropertyModel(this);
    }

    /**
     * @return the datastore in use
     */
    public Datastore datastore() {
        return datastore;
    }

    /**
     * Enables/disables the use of the discriminator during mapping
     *
     * @param discriminatorEnabled true if the discriminator should be used
     * @return this
     */
    public PropertyModelBuilder discriminatorEnabled(Boolean discriminatorEnabled) {
        this.discriminatorEnabled = discriminatorEnabled;
        return this;
    }

    /**
     * @return true if the discriminator is to be used
     */
    public Boolean discriminatorEnabled() {
        return discriminatorEnabled;
    }

    /**
     * Gets the annotation of this type.
     *
     * @param type the annotation class
     * @param <A>  the annotation type
     * @return the annotation instance or null if this annotation is on the field
     */
    @Nullable
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        for (Annotation annotation : annotations) {
            if (type.equals(annotation.annotationType())) {
                return type.cast(annotation);
            }
        }
        return null;
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
    public PropertyModelBuilder mappedName(String mappedName) {
        this.mappedName = mappedName;
        return this;
    }

    /**
     * @return the field's mapped name
     */
    public String mappedName() {
        return this.mappedName;
    }

    public int modifiers() {
        return modifiers;
    }

    public PropertyModelBuilder modifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    /**
     * @return the field name
     */
    public String name() {
        return name;
    }

    public PropertyModelBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the entity model owner
     *
     * @param entityModel the entity model
     * @return this
     * @since 2.1
     */
    public PropertyModelBuilder owner(EntityModel entityModel) {
        this.owner = entityModel;
        return this;
    }

    /**
     * @return the entity model owner
     * @since 2.1
     */
    public EntityModel owner() {
        return owner;
    }

    /**
     * Sets the {@link PropertySerialization} checker
     *
     * @param propertySerialization checks if a property should be serialized
     * @return this
     */
    public PropertyModelBuilder serialization(MorphiaPropertySerialization propertySerialization) {
        this.serialization = notNull("propertySerialization", propertySerialization);
        return this;
    }

    /**
     * @return the PropertySerialization for this property
     */
    public MorphiaPropertySerialization serialization() {
        return serialization;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PropertyModelBuilder.class.getSimpleName() + "[", "]")
                   .add("name='" + name + "'")
                   .add("mappedName='" + mappedName + "'")
                   .add("typeData=" + typeData)
                   .add("annotations=" + annotations)
                   .toString();
    }

    /**
     * @return the type data
     */
    public TypeData<?> typeData() {
        return typeData;
    }

    /**
     * Sets the type data
     *
     * @param typeData the type data
     * @return this
     */
    public PropertyModelBuilder typeData(TypeData<?> typeData) {
        this.typeData = notNull("typeData", typeData);
        return this;
    }
}
