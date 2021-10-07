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
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Handler;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
public final class PropertyModel {
    private final String name;
    private final TypeData<?> typeData;
    private final String mappedName;
    private Codec<? super Object> codec;
    private final PropertyAccessor<? super Object> accessor;
    private final MorphiaPropertySerialization serialization;
    private final Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private final List<String> loadNames; // List of stored names in order of trying, contains nameToStore and potential aliases
    private final EntityModel entityModel;
    private volatile Codec<? super Object> cachedCodec;
    private Class<?> normalizedType;

    PropertyModel(PropertyModelBuilder builder) {
        entityModel = builder.owner();
        name = Objects.requireNonNull(builder.name(), Sofia.notNull("name"));
        mappedName = Objects.requireNonNull(builder.mappedName(), Sofia.notNull("name"));
        typeData = Objects.requireNonNull(builder.typeData(), Sofia.notNull("typeData"));
        accessor = builder.accessor();
        serialization = builder.serialization();
        builder.annotations().forEach(ann -> annotationMap.put(ann.annotationType(), ann));
        configureCodec(builder.datastore());

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
     * Create a new {@link PropertyModelBuilder}
     *
     * @param datastore
     * @return the builder
     */
    static PropertyModelBuilder builder(Datastore datastore) {
        return new PropertyModelBuilder(datastore);
    }

    /**
     * @return the full name of the class plus java field name
     */
    public String getFullName() {
        return String.format("%s#%s", entityModel.getType().getName(), name);
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
     * @return the cached codec
     */
    @Nullable
    public Codec<? super Object> getCachedCodec() {
        return cachedCodec;
    }

    /**
     * @return the custom codec to use if set or null
     */
    @Nullable
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

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTypeData(), getMappedName(), getCodec(), getAccessor(), serialization,
            annotationMap.values(), getCachedCodec(), getNormalizedType());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertyModel)) {
            return false;
        }
        final PropertyModel that = (PropertyModel) o;
        return getName().equals(that.getName())
               && getTypeData().equals(that.getTypeData())
               && getMappedName().equals(that.getMappedName())
               && Objects.equals(getCodec(), that.getCodec())
               && getAccessor().equals(that.getAccessor())
               && serialization.equals(that.serialization)
               && Objects.equals(getCachedCodec(), that.getCachedCodec())
               && Objects.equals(getNormalizedType(), that.getNormalizedType());
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
    public String toString() {
        return new StringJoiner(", ", PropertyModel.class.getSimpleName() + "[", "]")
                   .add("name='" + name + "'")
                   .add("mappedName='" + mappedName + "'")
                   .add("typeData=" + typeData)
                   .add("annotations=" + annotationMap.values())
                   .toString();
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

    private void configureCodec(Datastore datastore) {
        Handler handler = getHandler();
        if (handler != null) {
            try {
                codec = handler.value()
                               .getDeclaredConstructor(Datastore.class, PropertyModel.class)
                               .newInstance(datastore, this);
            } catch (ReflectiveOperationException e) {
                throw new MappingException(e.getMessage(), e);
            }
        }
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

    /**
     * Checks a value against the configured rules for serialization
     *
     * @param value the value to check
     * @return true if the given value should be serialized
     */
    public boolean shouldSerialize(@Nullable Object value) {
        return serialization.shouldSerialize(value);
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

    void cachedCodec(Codec<? super Object> codec) {
        this.cachedCodec = codec;
    }

}
