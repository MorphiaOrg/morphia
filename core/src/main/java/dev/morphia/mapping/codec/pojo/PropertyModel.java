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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.mongodb.DBRef;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.Reference;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.references.MorphiaProxy;

import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertySerialization;

import static java.util.Arrays.asList;

/**
 * Represents a field on a class and stores various metadata such as generic parameters.
 *
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
@SuppressWarnings("removal")
public class PropertyModel {
    private boolean isFinal;

    private String name;
    private TypeData<?> typeData;
    private String mappedName;
    private PropertyAccessor<Object> accessor;
    private PropertySerialization serialization;
    private final Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private final List<String> loadNames = new ArrayList<>();
    private final EntityModel entityModel;
    // this property's position in its entity model, which addresses it in a CodecPlan; assigned when it's added
    private int slot = -1;
    private Class<?> normalizedType;
    // getType() with primitives boxed: a decoded value that's already an instance of this needs no conversion
    private Class<?> boxedType;
    private Conversions conversions;

    public PropertyModel(EntityModel entityModel) {
        this.entityModel = entityModel;
    }

    public PropertyModel(EntityModel owner, PropertyModel other) {
        entityModel = owner;
        conversions = other.conversions;

        name = other.name;
        typeData = other.typeData;
        mappedName = other.mappedName;
        accessor = other.accessor;
        annotationMap.putAll(other.annotationMap);
        loadNames.addAll(other.loadNames);
        serialization = other.serialization;
        normalizedType = other.normalizedType;
        isFinal = other.isFinal;
    }

    /**
     * Gets the parameterized type of a TypeData
     *
     * @param typeData the type to normalize
     * @return the unwrapped type
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static Class<?> normalize(TypeData<?> typeData) {
        while (!typeData.getTypeParameters().isEmpty()
                && (Collection.class.isAssignableFrom(typeData.getType())
                        || Map.class.isAssignableFrom(typeData.getType()))) {
            List<TypeData<?>> typeParameters = typeData.getTypeParameters();
            typeData = typeParameters.get(typeParameters.size() - 1);
        }
        Class<?> type = typeData.getType();

        while (type.isArray()) {
            type = type.getComponentType();
        }
        return type;
    }

    public void alternateNames(String... names) {
        loadNames.addAll(asList(names));
        for (String name : names) {
            // e.g. an alternate name that is also this property's mapped name
            if (entityModel.propertyModelsByMappedName.get(name) != this) {
                entityModel.propertyModelsByMappedName.put(name, this);
            }
        }
    }

    /**
     * @return the accessor to use when accessing this field
     */
    public final PropertyAccessor<Object> getAccessor() {
        return accessor;
    }

    public PropertyModel accessor(PropertyAccessor<? super Object> accessor) {
        this.accessor = accessor;
        return this;
    }

    /**
     * Find an annotation of a specific type or null if not found.
     *
     * @param type the annotation type to find
     * @param <A>  the class type
     * @return the annotation instance or null
     */
    @Nullable
    public final <A extends Annotation> A getAnnotation(Class<A> type) {
        return type.cast(annotationMap.get(type));
    }

    public final List<Annotation> getAnnotations() {
        return new ArrayList<>(annotationMap.values());
    }

    public PropertyModel annotation(Annotation annotation) {
        annotationMap.put(annotation.annotationType(), annotation);
        return this;
    }

    public PropertyModel annotations(List<Annotation> annotations) {
        annotations.forEach(ann -> annotationMap.put(ann.annotationType(), ann));
        return this;
    }

    public final boolean isFinal() {
        return isFinal;
    }

    public PropertyModel isFinal(boolean isFinal) {
        this.isFinal = isFinal;
        return this;
    }

    /**
     * @return the entity model owner of this field
     * @since 2.1
     */
    public final EntityModel getEntityModel() {
        return entityModel;
    }

    /**
     * @return the full name of the class plus java field name
     */
    public final String getFullName() {
        return String.format("%s#%s", entityModel.getType().getName(), name);
    }

    /**
     * @return the name of the field's (key)name for mongodb, in order of loading, as a read-only view that reflects later
     *         changes
     */
    public final List<String> getLoadNames() {
        return Collections.unmodifiableList(loadNames);
    }

    /**
     * @return the mapped name for the model
     */
    @NonNull
    public final String getMappedName() {
        return mappedName;
    }

    public PropertyModel mappedName(String name) {
        mappedName = name;
        return this;
    }

    /**
     * @return the field name for the model
     */
    public final String getName() {
        return name;
    }

    public PropertyModel name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the parameterized type of a List or the key type of a Map, e.g.
     *
     * @return the unwrapped type
     */
    public final Class<?> getNormalizedType() {
        if (normalizedType == null) {
            normalizedType = normalize(getTypeData());
        }

        return normalizedType;
    }

    /**
     * @return the type of this field
     */
    public final Class<?> getType() {
        return getTypeData().getType();
    }

    /**
     * @return the type data for the field
     */
    public final TypeData<?> getTypeData() {
        return typeData;
    }

    public PropertyModel typeData(TypeData<?> data) {
        typeData = data;
        return this;
    }

    /**
     * Gets the value of the property mapped on the instance given.
     *
     * @param instance the instance to use
     * @return the value stored in the property
     */
    @Nullable
    public Object getValue(Object instance) {
        Object target = instance;
        if (target instanceof MorphiaProxy) {
            target = ((MorphiaProxy) instance).unwrap();
            if (target == null) {
                return null;
            }
        }
        return getAccessor().get(target);
    }

    /**
     * Indicates whether the annotation is present in the mapping (does not check the java field annotations, just the ones discovered)
     *
     * @param type the annotation to search for
     * @return true if the annotation was found
     */
    public final boolean hasAnnotation(Class<? extends Annotation> type) {
        return annotationMap.containsKey(type);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PropertyModel.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("mappedName='" + getMappedName() + "'")
                .add("typeData=" + getTypeData())
                .add("annotations=" + getAnnotations())
                .toString();
    }

    /**
     * @return true if the MappedField is an array
     */
    public final boolean isArray() {
        return getType().isArray();
    }

    /**
     * @return true if the MappedField is a Map
     */
    public final boolean isMap() {
        return Map.class.isAssignableFrom(getTypeData().getType());
    }

    /**
     * @return true if this field is a reference to a foreign document
     * @see Reference
     * @see DBRef
     */
    public final boolean isReference() {
        return hasAnnotation(Reference.class) || DBRef.class == getType();
    }

    /**
     * @return true if this field is not a container type such as a List, Map, Set, or array
     */
    public final boolean isScalarValue() {
        return !isMap() && !isArray() && !isCollection();
    }

    /**
     * @return true if the MappedField is a Set
     */
    public final boolean isSet() {
        return Set.class.isAssignableFrom(getTypeData().getType());
    }

    /**
     * Sets the value for the java field
     *
     * @param instance the instance to update
     * @param value    the value to set
     */
    public void setValue(Object instance, @Nullable Object value) {
        if (boxedType == null) {
            boxedType = Conversions.boxedType(getType());
        }
        if (boxedType.isInstance(value)) {
            getAccessor().set(instance, value);
            return;
        }
        Conversions c = conversions != null ? conversions
                : new Conversions(Thread.currentThread().getContextClassLoader());
        getAccessor().set(instance, c.convert(value, getType()));
    }

    /**
     * Sets the Conversions instance to use for type conversions.
     *
     * @param conversions the Conversions instance
     * @return this PropertyModel
     */
    public PropertyModel conversions(Conversions conversions) {
        this.conversions = conversions;
        return this;
    }

    /**
     * Checks a value against the configured rules for serialization
     *
     * @param value the value to check
     * @return true if the given value should be serialized
     */
    public final boolean shouldSerialize(@Nullable Object value) {
        return serialization == null || serialization.shouldSerialize(value);
    }

    /**
     * @return true if this is a final property that's never stored because the configuration ignores finals
     * @morphia.internal
     */
    @MorphiaInternal
    public final boolean isIgnoredFinal() {
        return serialization instanceof MorphiaPropertySerialization morphia && morphia.isIgnoredFinal();
    }

    public PropertyModel serialization(PropertySerialization serialization) {
        this.serialization = serialization;
        return this;
    }

    public final boolean isCollection() {
        return Collection.class.isAssignableFrom(getTypeData().getType());
    }

    /**
     * @return this property's position in its entity model, or -1 if it hasn't been added to one
     */
    int slot() {
        return slot;
    }

    void slot(int slot) {
        this.slot = slot;
    }

}
