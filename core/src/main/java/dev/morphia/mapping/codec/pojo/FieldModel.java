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

import com.mongodb.DBRef;
import dev.morphia.Key;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertySerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Arrays.asList;

/**
 * Represents a field on a class and stores various metadata such as generic parameters.
 *
 * @morphia.internal
 * @since 2.0
 */
public final class FieldModel {
    private final Field field;
    private final String name;
    private final TypeData<?> typeData;
    private final String mappedName;
    private final Codec<? super Object> codec;
    private final PropertyAccessor<? super Object> accessor;
    private final PropertySerialization<? super Object> serialization;
    private final Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private final List<String> loadNames; // List of stored names in order of trying, contains nameToStore and potential aliases
    private final EntityModel entityModel;
    private volatile Codec<? super Object> cachedCodec;
    private Class<?> normalizedType;

    FieldModel(FieldModelBuilder builder) {
        entityModel = builder.entityModel();
        field = Objects.requireNonNull(builder.field(), Sofia.notNull("field"));
        name = Objects.requireNonNull(builder.name(), Sofia.notNull("name"));
        mappedName = Objects.requireNonNull(builder.mappedName(), Sofia.notNull("name"));
        typeData = Objects.requireNonNull(builder.typeData(), Sofia.notNull("typeData"));
        codec = builder.codec();
        cachedCodec = codec;
        accessor = builder.accessor();
        serialization = builder.serialization();

        field.setAccessible(true);
        builder.annotations().forEach(ann -> annotationMap.put(ann.annotationType(), ann));

        List<String> result;
        final AlsoLoad al = getAnnotation(AlsoLoad.class);
        if (al != null && al.value().length > 0) {
            final List<String> names = new ArrayList<>();
            names.add(getMappedName());
            names.addAll(asList(al.value()));
            result = names;
        } else {
            result = Collections.singletonList(getMappedName());
        }
        loadNames = result;
    }

    /**
     * Create a new {@link FieldModelBuilder}
     *
     * @return the builder
     */
    public static FieldModelBuilder builder() {
        return new FieldModelBuilder();
    }

    /**
     * Gets the parameterized type of a TypeData
     *
     * @param toNormalize the type to normalize
     * @return the unwrapped type
     * @morphia.internal
     */
    public static Class<?> normalize(TypeData<?> toNormalize) {
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
    public PropertyAccessor<? super Object> getAccessor() {
        return accessor;
    }

    /**
     * Find an annotation of a specific type or null if not found.
     *
     * @param type the annotation type to find
     * @param <A>  the class type
     * @return the annotation instance or null
     */
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return type.cast(annotationMap.get(type));
    }

    /**
     * @return the cached codec
     */
    public Codec<? super Object> getCachedCodec() {
        return cachedCodec;
    }

    /**
     * @return the custom codec to use if set or null
     */
    public Codec<?> getCodec() {
        return codec;
    }

    /**
     * @param document the Document get the value from
     * @return the value from first mapping of this field
     */
    public Object getDocumentValue(Document document) {
        return document.get(loadFromDocument(document));
    }

    /**
     * @return the entity model owner of this field
     * @since 2.1
     */
    public EntityModel getEntityModel() {
        return entityModel;
    }

    /**
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * @return the full name of the class plus java field name
     */
    public String getFullName() {
        Field field = getField();
        return String.format("%s#%s", field.getDeclaringClass().getName(), field.getName());
    }

    /**
     * @return the name of the field's (key)name for mongodb, in order of loading.
     */
    public List<String> getLoadNames() {
        return loadNames;
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
     * If the java field is a list/array/map then the sub-type T is returned (ex. List&lt;T&gt;, T[], Map&lt;?,T&gt;)
     *
     * @return the parameterized type of the field
     */
    public Class<?> getSpecializedType() {
        Class<?> specializedType;
        if (getType().isArray()) {
            specializedType = getType().getComponentType();
        } else {
            final List<TypeData<?>> typeParameters = getTypeData().getTypeParameters();
            specializedType = !typeParameters.isEmpty() ? typeParameters.get(0).getType() : null;
        }

        return specializedType;
    }

    /**
     * @return the type of this field
     */
    public Class<?> getType() {
        return getTypeData().getType();
    }

    /**
     * @return the type data for the field
     */
    public TypeData<?> getTypeData() {
        return typeData;
    }

    /**
     * Gets the value of the field mapped on the instance given.
     *
     * @param instance the instance to use
     * @return the value stored in the java field
     */
    public Object getValue(Object instance) {
        try {
            Object target = instance;
            if (target instanceof MorphiaProxy) {
                target = ((MorphiaProxy) instance).unwrap();
            }
            return getField().get(target);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    /**
     * Indicates whether the annotation is present in the mapping (does not check the java field annotations, just the ones discovered)
     *
     * @param type the annotation to search for
     * @return true if the annotation was found
     */
    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return annotationMap.containsKey(type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getField(), getName(), getTypeData(), getMappedName(), getCodec(), getAccessor(), serialization,
            annotationMap.values(), getCachedCodec(), getNormalizedType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldModel)) {
            return false;
        }
        final FieldModel that = (FieldModel) o;
        return getField().equals(that.getField())
               && getName().equals(that.getName())
               && getTypeData().equals(that.getTypeData())
               && getMappedName().equals(that.getMappedName())
               && Objects.equals(getCodec(), that.getCodec())
               && getAccessor().equals(that.getAccessor())
               && serialization.equals(that.serialization)
               && Objects.equals(getCachedCodec(), that.getCachedCodec())
               && Objects.equals(getNormalizedType(), that.getNormalizedType());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FieldModel.class.getSimpleName() + "[", "]")
                   .add("name='" + name + "'")
                   .add("mappedName='" + mappedName + "'")
                   .add("typeData=" + typeData)
                   .add("annotations=" + annotationMap.values())
                   .toString();
    }

    /**
     * @return true if the MappedField is an array
     */
    public boolean isArray() {
        return getType().isArray();
    }

    /**
     * @return true if the MappedField is a Map
     */
    public boolean isMap() {
        return Map.class.isAssignableFrom(getTypeData().getType());
    }

    /**
     * @return true if this field is a container type such as a List, Map, Set, or array
     */
    public boolean isMultipleValues() {
        return !isScalarValue();
    }

    /**
     * @return true if this field is a reference to a foreign document
     * @see Reference
     * @see Key
     * @see DBRef
     */
    public boolean isReference() {
        return hasAnnotation(Reference.class) || Key.class == getType() || DBRef.class == getType();
    }

    /**
     * @return true if this field is not a container type such as a List, Map, Set, or array
     */
    public boolean isScalarValue() {
        return !isMap() && !isArray() && !isCollection();
    }

    /**
     * @return true if the MappedField is a Set
     */
    public boolean isSet() {
        return Set.class.isAssignableFrom(getTypeData().getType());
    }

    /**
     * @return true if this field is marked as transient
     */
    public boolean isTransient() {
        return !hasAnnotation(Transient.class)
               && !hasAnnotation(java.beans.Transient.class)
               && Modifier.isTransient(getType().getModifiers());
    }

    /**
     * Sets the value for the java field
     *
     * @param instance the instance to update
     * @param value    the value to set
     */
    public void setValue(Object instance, Object value) {
        try {
            final Field field = getField();
            field.set(instance, Conversions.convert(value, field.getType()));
        } catch (IllegalAccessException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    /**
     * Checks a value against the configured rules for serialization
     *
     * @param value the value to check
     * @return true if the given value should be serialized
     */
    public boolean shouldSerialize(Object value) {
        return serialization.shouldSerialize(value);
    }

    private boolean isCollection() {
        return Collection.class.isAssignableFrom(getTypeData().getType());
    }

    private String loadFromDocument(Document document) {
        String mappedFieldName = getMappedName();
        if (document.containsKey(mappedFieldName)) {
            return mappedFieldName;
        }
        for (String name : getLoadNames()) {
            if (document.containsKey(name)) {
                return name;
            }
        }

        return null;
    }

    void cachedCodec(Codec<? super Object> codec) {
        this.cachedCodec = codec;
    }

}
