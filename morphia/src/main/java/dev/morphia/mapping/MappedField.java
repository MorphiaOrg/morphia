/*
 * Copyright 2008-2016 MongoDB, Inc.
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

package dev.morphia.mapping;


import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.morphia.Key;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.ConstructorArgs;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.NotSaved;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Serialized;
import dev.morphia.annotations.Text;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;


/**
 * @morphia.internal
 * @deprecated
 */
@SuppressWarnings("unchecked")
public class MappedField {
    private static final Logger LOG = LoggerFactory.getLogger(MappedField.class);
    // The Annotations to look for when reflecting on the field (stored in the mappingAnnotations)
    private static final List<Class<? extends Annotation>> INTERESTING = new ArrayList<Class<? extends Annotation>>();

    static {
        INTERESTING.add(Serialized.class);
        INTERESTING.add(Indexed.class);
        INTERESTING.add(Property.class);
        INTERESTING.add(Reference.class);
        INTERESTING.add(Embedded.class);
        INTERESTING.add(Id.class);
        INTERESTING.add(Version.class);
        INTERESTING.add(ConstructorArgs.class);
        INTERESTING.add(AlsoLoad.class);
        INTERESTING.add(NotSaved.class);
        INTERESTING.add(Text.class);
    }

    // Annotations that have been found relevant to mapping
    private final Map<Class<? extends Annotation>, Annotation> foundAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    private final List<MappedField> typeParameters = new ArrayList<MappedField>();
    private Class persistedClass;
    private Field field; // the field :)
    private Class realType; // the real type
    private Constructor constructor; // the constructor for the type
    private Type subType; // the type (T) for the Collection<T>/T[]/Map<?,T>
    private Type mapKeyType; // the type (T) for the Map<T,?>
    private boolean isSingleValue = true; // indicates the field is a single value
    private boolean isMongoType;
    // indicated the type is a mongo compatible type (our version of value-type)
    private boolean isMap; // indicated if it implements Map interface
    private boolean isSet; // indicated if the collection is a set
    //for debugging
    private boolean isArray; // indicated if it is an Array
    private boolean isCollection; // indicated if the collection is a list)
    private Type genericType;

    private String nameToStore; // the field name in the db.
    private List<String> loadNames; // List of stored names in order of trying, contains nameToStore and potential aliases

    MappedField(final Field f, final Class<?> clazz, final Mapper mapper) {
        f.setAccessible(true);
        field = f;
        persistedClass = clazz;
        realType = field.getType();
        genericType = field.getGenericType();
        discover(mapper);
        discoverNames();
    }

    /**
     * Creates a MappedField
     *
     * @param field   the Type for the field
     * @param type   the Type for the field
     * @param mapper the Mapper to use
     */
    MappedField(final Field field, final Type type, final Mapper mapper) {
        this.field = field;
        genericType = type;
        discoverType(mapper);
        discoverNames();
    }

    private void discoverNames() {
        nameToStore = getMappedFieldName();
        loadNames = inferLoadNames();
    }

    /**
     * Adds an annotation for Morphia to retain when mapping.
     *
     * @param annotation the type to retain
     */
    public static void addInterestingAnnotation(final Class<? extends Annotation> annotation) {
        INTERESTING.add(annotation);
    }

    /**
     * Adds the annotation, if it exists on the field.
     *
     * @param clazz the annotation to add
     */
    public void addAnnotation(final Class<? extends Annotation> clazz) {
        if (field.isAnnotationPresent(clazz)) {
            addAnnotation(clazz, field.getAnnotation(clazz));
        }
    }

    /**
     * Adds the annotation, if it exists on the field.
     *
     * @param clazz type of the annotation
     * @param ann   the annotation
     */
    public void addAnnotation(final Class<? extends Annotation> clazz, final Annotation ann) {
        foundAnnotations.put(clazz, ann);
        discoverNames();
    }

    /**
     * @param clazz the annotation to search for
     * @param <T>   the type of the annotation
     * @return the annotation instance if it exists on this field
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
        return (T) foundAnnotations.get(clazz);
    }

    /**
     * @return the annotations found while mapping
     */
    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return Collections.unmodifiableMap(foundAnnotations);
    }

    /**
     * @return a constructor for the type represented by the field
     */
    public Constructor getCTor() {
        return constructor;
    }

    /**
     * @return the concrete type of the MappedField
     */
    public Class getConcreteType() {
        final Embedded e = getAnnotation(Embedded.class);
        if (e != null) {
            final Class concrete = e.concreteClass();
            if (concrete != Object.class) {
                return concrete;
            }
        }

        final Property p = getAnnotation(Property.class);
        if (p != null) {
            final Class concrete = p.concreteClass();
            if (concrete != Object.class) {
                return concrete;
            }
        }
        return getType();
    }

    /**
     * @param dbObj the DBObject get the value from
     * @return the value from best mapping of this field
     */
    public Object getDbObjectValue(final DBObject dbObj) {
        return dbObj.get(getFirstFieldName(dbObj));
    }

    /**
     * @return the declaring class of the java field
     */
    public Class getDeclaringClass() {
        return field.getDeclaringClass();
    }

    /**
     * @return the underlying java field
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets the value of the field mapped on the instance given.
     *
     * @param instance the instance to use
     * @return the value stored in the java field
     */
    public Object getFieldValue(final Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the field name to use when converting from a DBObject
     *
     * @param dbObj the DBObject to scan for alternate names
     * @return the value of this field mapped from the DBObject
     * @see AlsoLoad
     */
    public String getFirstFieldName(final DBObject dbObj) {
        String fieldName = getNameToStore();
        boolean foundField = false;
        for (final String n : getLoadNames()) {
            if (dbObj.containsField(n)) {
                if (!foundField) {
                    foundField = true;
                    fieldName = n;
                } else {
                    throw new MappingException(format("Found more than one field from @AlsoLoad %s", getLoadNames()));
                }
            }
        }
        return fieldName;
    }

    /**
     * @return the full name of the class plus java field name
     */
    public String getFullName() {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    /**
     * @return the name of the java field, as declared on the class
     */
    public String getJavaFieldName() {
        return field.getName();
    }

    /**
     * @return the name of the field's (key)name for mongodb, in order of loading.
     */
    public List<String> getLoadNames() {
        return loadNames;
    }

    protected List<String> inferLoadNames() {
        final AlsoLoad al = (AlsoLoad) foundAnnotations.get(AlsoLoad.class);
        if (al != null && al.value() != null && al.value().length > 0) {
            final List<String> names = new ArrayList<String>();
            names.add(getMappedFieldName());
            names.addAll(asList(al.value()));
            return names;
        } else {
            return Collections.singletonList(getMappedFieldName());
        }
    }

    /**
     * If the underlying java type is a map then it returns T from Map&lt;T,V&gt;
     *
     * @return the type of the map key
     */
    public Class getMapKeyClass() {
        return toClass(mapKeyType);
    }

    /**
     * @return the name of the field's (key)name for mongodb
     */
    public String getNameToStore() {
        return nameToStore;
    }

    /**
     * If the java field is a list/array/map then the sub-type T is returned (ex. List&lt;T&gt;, T[], Map&lt;?,T&gt;
     *
     * @return the parameterized type of the field
     */
    public Class getSubClass() {
        return toClass(subType);
    }

    /**
     * If the java field is a list/array/map then the sub-type T is returned (ex. List&lt;T&gt;, T[], Map&lt;?,T&gt;
     *
     * @return the parameterized type of the field
     */
    public Type getSubType() {
        return subType;
    }

    /**
     * @return true if this field is marked as transient
     */
    public boolean isTransient() {
        return hasAnnotation(Transient.class) || Modifier.isTransient(field.getModifiers());
    }

    void setSubType(final Type subType) {
        this.subType = subType;
    }

    /**
     * @return the type of the underlying java field
     */
    public Class getType() {
        return realType;
    }

    /**
     * @return the type parameters defined on the field
     */
    public List<MappedField> getTypeParameters() {
        return typeParameters;
    }

    /**
     * Indicates whether the annotation is present in the mapping (does not check the java field annotations, just the ones discovered)
     *
     * @param ann the annotation to search for
     * @return true if the annotation was found
     */
    public boolean hasAnnotation(final Class ann) {
        return foundAnnotations.containsKey(ann);
    }

    /**
     * @return true if the MappedField is an array
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * @return true if the MappedField is a Map
     */
    public boolean isMap() {
        return isMap;
    }

    /**
     * @return true if this field is a container type such as a List, Map, Set, or array
     */
    public boolean isMultipleValues() {
        return !isSingleValue();
    }

    /**
     * @return true if this field is a reference to a foreign document
     * @see Reference
     * @see Key
     * @see DBRef
     */
    public boolean isReference() {
        return hasAnnotation(Reference.class) || Key.class == getConcreteType() || DBRef.class == getConcreteType()
               || MorphiaReference.class == getConcreteType();
    }

    /**
     * @return true if the MappedField is a Set
     */
    public boolean isSet() {
        return isSet;
    }

    /**
     * @return true if this field is not a container type such as a List, Map, Set, or array
     */
    public boolean isSingleValue() {
        if (!isSingleValue && !isMap && !isSet && !isArray && !isCollection) {
            throw new RuntimeException("Not single, but none of the types that are not-single.");
        }
        return isSingleValue;
    }

    /**
     * @return true if type is understood by MongoDB and the driver
     */
    public boolean isTypeMongoCompatible() {
        return isMongoType;
    }

    /**
     * Adds the annotation even if not on the declared class/field.
     *
     * @param ann the annotation to add
     * @return ann the annotation
     * @deprecated unused
     */
    @Deprecated
    public Annotation putAnnotation(final Annotation ann) {
        Annotation put = foundAnnotations.put(ann.getClass(), ann);
        discoverNames();
        return put;
    }

    /**
     * Sets the value for the java field
     *
     * @param instance the instance to update
     * @param value    the value to set
     */
    public void setFieldValue(final Object instance, final Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getNameToStore()).append(" (");
        sb.append(" type:").append(realType.getSimpleName()).append(",");

        if (isSingleValue()) {
            sb.append(" single:true,");
        } else {
            sb.append(" multiple:true,");
            sb.append(" subtype:").append(getSubClass()).append(",");
        }
        if (isMap()) {
            sb.append(" map:true,");
            if (getMapKeyClass() != null) {
                sb.append(" map-key:").append(getMapKeyClass().getSimpleName());
            } else {
                sb.append(" map-key: class unknown! ");
            }
        }

        if (isSet()) {
            sb.append(" set:true,");
        }
        if (isCollection) {
            sb.append(" collection:true,");
        }
        if (isArray) {
            sb.append(" array:true,");
        }

        //remove last comma
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }

        sb.append("); ").append(foundAnnotations.toString());
        return sb.toString();
    }

    /**
     * Discovers interesting (that we care about) things about the field.
     */
    protected void discover(final Mapper mapper) {
        for (final Class<? extends Annotation> clazz : INTERESTING) {
            addAnnotation(clazz);
        }

        //type must be discovered before the constructor.
        discoverType(mapper);
        constructor = discoverConstructor();
        discoverMultivalued();

        // check the main type
        isMongoType = ReflectionUtils.isPropertyType(realType);

        // if the main type isn't supported by the Mongo, see if the subtype is.
        // works for T[], List<T>, Map<?, T>, where T is Long/String/etc.
        if (!isMongoType && subType != null) {
            isMongoType = ReflectionUtils.isPropertyType(subType);
        }

        if (!isMongoType && !isSingleValue && (subType == null || subType == Object.class)) {
            if (LOG.isWarnEnabled() && !mapper.getConverters().hasDbObjectConverter(this)) {
                LOG.warn(format("The multi-valued field '%s' is a possible heterogeneous collection. It cannot be verified. "
                                   + "Please declare a valid type to get rid of this warning. %s", getFullName(), subType));
            }
            isMongoType = true;
        }
    }

    @SuppressWarnings("unchecked")
    protected void discoverType(final Mapper mapper) {
        if (genericType instanceof TypeVariable) {
            realType = extractTypeVariable((TypeVariable) genericType);
        } else if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            final Type[] types = pt.getActualTypeArguments();
            realType = toClass(pt);

            collectTypeParameters(mapper, types);
        } else if (genericType instanceof WildcardType) {
            final WildcardType wildcardType = (WildcardType) genericType;
            final Type[] types = wildcardType.getUpperBounds();
            realType = toClass(types[0]);
        } else if (genericType instanceof Class) {
            realType = (Class) genericType;
        } else if (genericType instanceof GenericArrayType) {
            final Type genericComponentType = ((GenericArrayType) genericType).getGenericComponentType();
            if (genericComponentType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericComponentType;
                realType = toClass(genericType);

                final Type[] types = pt.getActualTypeArguments();
                collectTypeParameters(mapper, types);
            } else {
                if (genericComponentType instanceof TypeVariable) {
                    realType = toClass(genericType);
                } else {
                    realType = (Class) genericComponentType;
                }
            }
        }

        if (Object.class.equals(realType) || Object[].class.equals(realType)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(format("Parameterized types are treated as untyped Objects. See field '%s' on %s", field.getName(),
                                   field.getDeclaringClass()));
            }
        }

        if (realType == null) {
            throw new MappingException(format("A type could not be found for the field %s.%s", getType(), getField()));
        }
    }

    private void collectTypeParameters(final Mapper mapper, final Type[] types) {
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                typeParameters.add(new EphemeralMappedField((ParameterizedType) type, this, mapper));
            } else {
                if (type instanceof WildcardType) {
                    type = ((WildcardType) type).getUpperBounds()[0];
                }
                typeParameters.add(new EphemeralMappedField(type, this, mapper));
            }
        }
    }

    private Class extractTypeVariable(final TypeVariable<?> type) {
        final Class typeArgument = ReflectionUtils.getTypeArgument(persistedClass, type);
        return typeArgument != null ? typeArgument : Object.class;
    }

    /**
     * @return the name of the field's key-name for mongodb
     * @morphia.internal
     */
    public String getMappedFieldName() {
        if (hasAnnotation(Id.class)) {
            return "_id";
        } else if (hasAnnotation(Property.class)) {
            final Property mv = (Property) foundAnnotations.get(Property.class);
            if (!mv.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mv.value();
            }
        } else if (hasAnnotation(Reference.class)) {
            final Reference mr = (Reference) foundAnnotations.get(Reference.class);
            if (!mr.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mr.value();
            }
        } else if (hasAnnotation(Embedded.class)) {
            final Embedded me = (Embedded) foundAnnotations.get(Embedded.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        } else if (hasAnnotation(Serialized.class)) {
            final Serialized me = (Serialized) foundAnnotations.get(Serialized.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        } else if (hasAnnotation(Version.class)) {
            final Version me = (Version) foundAnnotations.get(Version.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        }

        return field.getName();
    }

    protected Class toClass(final Type t) {
        if (t == null) {
            return null;
        } else if (t instanceof Class) {
            return (Class) t;
        } else if (t instanceof GenericArrayType) {
            final Type type = ((GenericArrayType) t).getGenericComponentType();
            Class aClass;
            if (type instanceof ParameterizedType) {
                aClass = (Class) ((ParameterizedType) type).getRawType();
            } else if (type instanceof TypeVariable) {
                aClass = ReflectionUtils.getTypeArgument(persistedClass, (TypeVariable<?>) type);
                if (aClass == null) {
                    aClass = Object.class;
                }
            } else {
                aClass = (Class) type;
            }
            return Array.newInstance(aClass, 0).getClass();
        } else if (t instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) t).getRawType();
        } else if (t instanceof WildcardType) {
            return (Class) ((WildcardType) t).getUpperBounds()[0];
        }

        throw new RuntimeException("Generic TypeVariable not supported!");

    }

    private Constructor discoverConstructor() {
        Class<?> type = null;
        // get the first annotation with a concreteClass that isn't Object.class
        for (final Annotation an : foundAnnotations.values()) {
            try {
                final Method m = an.getClass().getMethod("concreteClass");
                m.setAccessible(true);
                final Object o = m.invoke(an);
                //noinspection EqualsBetweenInconvertibleTypes
                if (o != null && !(o.equals(Object.class))) {
                    type = (Class) o;
                    break;
                }
            } catch (NoSuchMethodException e) {
                // do nothing
            } catch (IllegalArgumentException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("There should not be an argument", e);
                }
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("", e);
                }
            }
        }

        if (type != null) {
            try {
                constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                if (!hasAnnotation(ConstructorArgs.class)) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("No usable constructor for " + type.getName(), e);
                    }
                }
            }
        } else {
            // see if we can create instances of the type used for declaration
            type = getType();

            // short circuit to avoid wasting time throwing an exception trying to get a constructor we know doesnt exist
            if (type == List.class || type == Map.class) {
                return null;
            }

            if (type != null) {
                try {
                    constructor = type.getDeclaredConstructor();
                    constructor.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    // never mind.
                } catch (SecurityException e) {
                    // never mind.
                }
            }
        }
        return constructor;
    }

    private void discoverMultivalued() {
        isMap = Map.class.isAssignableFrom(realType);
        isSet = Set.class.isAssignableFrom(realType);
        //for debugging
        isCollection = Collection.class.isAssignableFrom(realType);
        isArray = realType.isArray();

        if (isArray || isCollection || isMap || isSet || GenericArrayType.class.isAssignableFrom(genericType.getClass())) {

            isSingleValue = false;

            // get the subtype T, T[]/List<T>/Map<?,T>; subtype of Long[], List<Long> is Long
            subType = (realType.isArray()) ? realType.getComponentType() : ReflectionUtils.getParameterizedType(field, isMap ? 1 : 0);

            if (isMap) {
                mapKeyType = ReflectionUtils.getParameterizedType(field, 0);
            }
        }
    }

    void setIsMap(final boolean isMap) {
        this.isMap = isMap;
    }

    void setIsMongoType(final boolean isMongoType) {
        this.isMongoType = isMongoType;
    }

    void setIsSet(final boolean isSet) {
        this.isSet = isSet;
    }

    void setMapKeyType(final Class mapKeyType) {
        this.mapKeyType = mapKeyType;
    }
}
