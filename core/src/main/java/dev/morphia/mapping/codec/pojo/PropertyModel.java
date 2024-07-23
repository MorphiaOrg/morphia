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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import com.mongodb.DBRef;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Handler;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.mapping.experimental.MorphiaReference;

import org.bson.Document;
import org.bson.codecs.Codec;
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
    private Codec<? super Object> codec;
    private Class<?> normalizedType;

    public PropertyModel(EntityModel entityModel) {
        this.entityModel = entityModel;
    }

    public PropertyModel(EntityModel owner, PropertyModel other) {
        entityModel = owner;

        name = other.name;
        typeData = other.typeData;
        mappedName = other.mappedName;
        accessor = other.accessor;
        annotationMap.putAll(other.annotationMap);
        loadNames.addAll(other.loadNames);
        serialization = other.serialization;
        normalizedType = other.normalizedType;
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
                        || Map.class.isAssignableFrom(typeData.getType())
                        || MorphiaReference.class.isAssignableFrom(typeData.getType()))) {
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
            entityModel.propertyModelsByMappedName.put(name, this);
        }
    }

    /**
     * @return the accessor to use when accessing this field
     */
    public PropertyAccessor<Object> getAccessor() {
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
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return type.cast(annotationMap.get(type));
    }

    public List<Annotation> getAnnotations() {
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

    public boolean isFinal() {
        return isFinal;
    }

    public PropertyModel isFinal(boolean isFinal) {
        this.isFinal = isFinal;
        return this;
    }

    Codec<?> getCodec() {
        return codec;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTypeData(), getMappedName(), codec, getAccessor(), serialization,
                annotationMap.values(), getNormalizedType());
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
     * @return the full name of the class plus java field name
     */
    public String getFullName() {
        return String.format("%s#%s", entityModel.getType().getName(), name);
    }

    /**
     * @return the name of the field's (key)name for mongodb, in order of loading.
     */
    public List<String> getLoadNames() {
        return new ArrayList<>(loadNames);
    }

    /**
     * @return the mapped name for the model
     */
    @NonNull
    public String getMappedName() {
        return mappedName;
    }

    public PropertyModel mappedName(String name) {
        mappedName = name;
        return this;
    }

    /**
     * @return the field name for the model
     */
    public String getName() {
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
    public Class<?> getNormalizedType() {
        if (normalizedType == null) {
            normalizedType = normalize(getTypeData());
        }

        return normalizedType;
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
        }
        return accessor.get(target);
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertyModel that)) {
            return false;
        }
        return getName().equals(that.getName())
                && getTypeData().equals(that.getTypeData())
                && getMappedName().equals(that.getMappedName())
                && Objects.equals(codec, that.codec)
                && getAccessor().equals(that.getAccessor())
                && serialization.equals(that.serialization)
                && Objects.equals(getNormalizedType(), that.getNormalizedType());
    }

    /**
     * @param datastore
     * @return the custom codec to use if set or null
     */
    @Nullable
    public Codec<?> specializeCodec(Datastore datastore) {
        if (codec == null) {
            configureCodec(datastore);
        }
        return codec;
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
     * @see DBRef
     */
    public boolean isReference() {
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
    public void setValue(Object instance, @Nullable Object value) {
        accessor.set(instance, Conversions.convert(value, getType()));
    }

    /**
     * Checks a value against the configured rules for serialization
     *
     * @param value the value to check
     * @return true if the given value should be serialized
     */
    public final boolean shouldSerialize(@Nullable Object value) {
        return serialization.shouldSerialize(value);
    }

    public PropertyModel serialization(PropertySerialization serialization) {
        this.serialization = serialization;
        return this;
    }

    private void configureCodec(Datastore datastore) {
        Handler handler = getHandler();
        if (handler != null) {
            try {
                codec = handler.value()
                        .getDeclaredConstructor(MorphiaDatastore.class, PropertyModel.class)
                        .newInstance(datastore, this);
            } catch (ReflectiveOperationException e) {
                throw new MappingException(e.getMessage(), e);
            }
        } else if (typeData.getTypeParameters().isEmpty()) {
            codec = (Codec<? super Object>) ((MorphiaDatastore) datastore).getCodecRegistry().get(getType());
        }
    }

    @Nullable
    private Handler getHandler() {
        Handler handler = typeData.getType().getAnnotation(Handler.class);

        if (handler == null) {
            handler = (Handler) annotationMap.values()
                    .stream().filter(a -> a.getClass().equals(Handler.class))
                    .findFirst().orElse(null);
            if (handler == null) {
                Iterator<Annotation> iterator = annotationMap.values().iterator();
                while (handler == null && iterator.hasNext()) {
                    handler = iterator.next().annotationType().getAnnotation(Handler.class);
                }
            }
        }

        return handler;
    }

    private boolean isCollection() {
        return Collection.class.isAssignableFrom(getTypeData().getType());
    }

    @Nullable
    private String loadFromDocument(Document document) {
        String propertyName = getMappedName();
        if (document.containsKey(propertyName)) {
            return propertyName;
        }
        for (String name : getLoadNames()) {
            if (document.containsKey(name)) {
                return name;
            }
        }

        return null;
    }

    void codec(Codec<? super Object> codec) {
        this.codec = codec;
    }

}
