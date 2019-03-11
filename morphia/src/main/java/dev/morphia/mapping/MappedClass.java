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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.PreSave;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Serialized;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.validation.MappingValidator;
import dev.morphia.utils.ReflectionUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;


/**
 * @morphia.internal
 * @deprecated
 */
@Deprecated
public class MappedClass {
    private static final Logger LOG = LoggerFactory.getLogger(MappedClass.class);
    /**
     * Annotations we are interested in looking for.
     *
     * @see #addInterestingAnnotation
     */
    private static final List<Class<? extends Annotation>> INTERESTING_ANNOTATIONS = new ArrayList<Class<? extends Annotation>>();
    /**
     * Annotations interesting for life-cycle events
     */
    @SuppressWarnings("unchecked")
    private static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = asList(PrePersist.class,
                                                                                          PreSave.class,
                                                                                          PreLoad.class,
                                                                                          PostPersist.class,
                                                                                          PostLoad.class);

    static {
        INTERESTING_ANNOTATIONS.add(Embedded.class);
        INTERESTING_ANNOTATIONS.add(Entity.class);
        INTERESTING_ANNOTATIONS.add(EntityListeners.class);
        INTERESTING_ANNOTATIONS.add(Version.class);
        INTERESTING_ANNOTATIONS.add(Converters.class);
        INTERESTING_ANNOTATIONS.add(Indexes.class);
        INTERESTING_ANNOTATIONS.add(Validation.class);
        INTERESTING_ANNOTATIONS.add(Field.class);
        INTERESTING_ANNOTATIONS.add(IndexOptions.class);
    }

    /**
     * Annotations we were interested in, and found.
     */
    private final Map<Class<? extends Annotation>, List<Annotation>> foundAnnotations =
        new HashMap<Class<? extends Annotation>, List<Annotation>>();
    /**
     * Methods which are life-cycle events
     */
    private final Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods =
        new HashMap<Class<? extends Annotation>, List<ClassMethodPair>>();
    /**
     * a list of the fields to map
     */
    private final List<MappedField> persistenceFields = new ArrayList<MappedField>();
    /**
     * the type we are mapping to/from
     */
    private final Class<?> clazz;
    /**
     * special fields representing the Key of the object
     */
    private java.lang.reflect.Field idField;
    /**
     * special annotations representing the type the object
     */
    private Entity entityAn;
    private Embedded embeddedAn;
    private MapperOptions mapperOptions;
    private MappedClass superClass;
    private List<MappedClass> interfaces = new ArrayList<MappedClass>();

    /**
     * Creates a MappedClass instance
     *
     * @param clazz  the class to be mapped
     * @param mapper the Mapper to use
     */
    public MappedClass(final Class<?> clazz, final Mapper mapper) {
        this.clazz = clazz;
        mapperOptions = mapper.getOptions();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating MappedClass for " + clazz);
        }

        basicValidate();
        discover(mapper);

        if (LOG.isDebugEnabled()) {
            LOG.debug("MappedClass done: " + toString());
        }
    }

    /**
     * This is an internal method subject to change without notice.
     *
     * @return the parent class of this type if there is one null otherwise
     *
     * @since 1.3
     */
    public MappedClass getSuperClass() {
        return superClass;
    }


    /**
     * @return true if the MappedClass is an interface
     */
    public boolean isInterface() {
        return clazz.isInterface();
    }

    /**
     * This is an internal method subject to change without notice.
     *
     * @return true if the MappedClass is abstract
     * @since 1.3
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Checks to see if it a Map/Set/List or a property supported by the MongoDB java driver
     *
     * @param clazz the type to check
     * @return true if the type is supported
     */
    public static boolean isSupportedType(final Class<?> clazz) {
        if (ReflectionUtils.isPropertyType(clazz)) {
            return true;
        }
        if (clazz.isArray() || Map.class.isAssignableFrom(clazz) || Iterable.class.isAssignableFrom(clazz)) {
            Class<?> subType;
            if (clazz.isArray()) {
                subType = clazz.getComponentType();
            } else {
                subType = ReflectionUtils.getParameterizedClass(clazz);
            }

            //get component type, String.class from List<String>
            if (subType != null && subType != Object.class && !ReflectionUtils.isPropertyType(subType)) {
                return false;
            }

            //either no componentType or it is an allowed type
            return true;
        }
        return false;
    }

    /**
     * Adds an annotation for Morphia to retain when mapping.
     *
     * @param annotation the type to retain
     */
    public static void addInterestingAnnotation(final Class<? extends Annotation> annotation) {
        INTERESTING_ANNOTATIONS.add(annotation);
    }

    /**
     * Adds the given Annotation to the internal list for the given Class.
     *
     * @param clazz the type to add
     * @param ann   the annotation to add
     */
    public void addAnnotation(final Class<? extends Annotation> clazz, final Annotation ann) {
        if (ann == null || clazz == null) {
            return;
        }

        if (!foundAnnotations.containsKey(clazz)) {
            foundAnnotations.put(clazz, new ArrayList<Annotation>());
        }

        foundAnnotations.get(clazz).add(ann);
    }

    /**
     * Call the lifecycle methods
     *
     * @param event  the lifecycle annotation
     * @param entity the entity to process
     * @param dbObj  the dbObject to use
     * @param mapper  the Mapper to use
     * @return dbObj
     */
    @SuppressWarnings({"WMI", "unchecked"})
    public DBObject callLifecycleMethods(final Class<? extends Annotation> event, final Object entity, final DBObject dbObj,
                                         final Mapper mapper) {
        final List<ClassMethodPair> methodPairs = getLifecycleMethods((Class<Annotation>) event);
        DBObject retDbObj = dbObj;
        try {
            Object tempObj;
            if (methodPairs != null) {
                final HashMap<Class<?>, Object> toCall = new HashMap<Class<?>, Object>((int) (methodPairs.size() * 1.3));
                for (final ClassMethodPair cm : methodPairs) {
                    toCall.put(cm.clazz, null);
                }
                for (final Class<?> c : toCall.keySet()) {
                    if (c != null) {
                        toCall.put(c, getOrCreateInstance(c, mapper));
                    }
                }

                for (final ClassMethodPair cm : methodPairs) {
                    final Method method = cm.method;
                    final Object inst = toCall.get(cm.clazz);
                    method.setAccessible(true);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug(format("Calling lifecycle method(@%s %s) on %s", event.getSimpleName(), method, inst));
                    }

                    if (inst == null) {
                        if (method.getParameterTypes().length == 0) {
                            tempObj = method.invoke(entity);
                        } else {
                            tempObj = method.invoke(entity, retDbObj);
                        }
                    } else if (method.getParameterTypes().length == 0) {
                        tempObj = method.invoke(inst);
                    } else if (method.getParameterTypes().length == 1) {
                        tempObj = method.invoke(inst, entity);
                    } else {
                        tempObj = method.invoke(inst, entity, retDbObj);
                    }

                    if (tempObj != null) {
                        retDbObj = (DBObject) tempObj;
                    }
                }
            }

            callGlobalInterceptors(event, entity, dbObj, mapper);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return retDbObj;
    }

    /**
     * Check java field name that will be stored in mongodb
     *
     * @param name the name to search for
     * @return true if a Java field with that name is found
     */
    public boolean containsJavaFieldName(final String name) {
        return getMappedField(name) != null;
    }

    /**
     * Looks for an annotation of the type given
     *
     * @param clazz the type to search for
     * @return the instance if it was found, if more than one was found, the last one added
     */
    public Annotation getAnnotation(final Class<? extends Annotation> clazz) {
        final List<Annotation> found = foundAnnotations.get(clazz);
        return found == null || found.isEmpty() ? null : found.get(found.size() - 1);
    }

    /**
     * Looks for an annotation in the annotations found on a class while mapping
     *
     * @param clazz the class to search for
     * @param <T>   the type of annotation to find
     * @return the instance if it was found, if more than one was found, the last one added
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getAnnotations(final Class<? extends Annotation> clazz) {
        return (List<T>) foundAnnotations.get(clazz);
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * @return the collName
     */
    public String getCollectionName() {
        if (entityAn == null || entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
            return mapperOptions.isUseLowerCaseCollectionNames() ? clazz.getSimpleName().toLowerCase() : clazz.getSimpleName();
        }
        return entityAn.value();
    }

    /**
     * @return the embeddedAn
     */
    public Embedded getEmbeddedAnnotation() {
        return embeddedAn;
    }

    /**
     * @return the entityAn
     */
    public Entity getEntityAnnotation() {
        return entityAn;
    }

    /**
     * Returns fields annotated with the clazz
     *
     * @param clazz The Annotation to find.
     * @return the list of fields
     */
    public List<MappedField> getFieldsAnnotatedWith(final Class<? extends Annotation> clazz) {
        final List<MappedField> results = new ArrayList<MappedField>();
        for (final MappedField mf : persistenceFields) {
            if (mf.hasAnnotation(clazz)) {
                results.add(mf);
            }
        }
        return results;
    }

    /**
     * Returns the first found Annotation, or null.
     *
     * @param clazz The Annotation to find.
     * @return First found Annotation or null of none found.
     */
    public Annotation getFirstAnnotation(final Class<? extends Annotation> clazz) {
        final List<Annotation> found = foundAnnotations.get(clazz);
        return found == null || found.isEmpty() ? null : found.get(0);
    }

    /**
     * @return the idField
     */
    public java.lang.reflect.Field getIdField() {
        return idField;
    }

    private List<ClassMethodPair> getLifecycleMethods(final Class<Annotation> clazz) {
        return lifecycleMethods.get(clazz);
    }

    /**
     * Returns the MappedField by the name that it will stored in mongodb as
     *
     * @param storedName the name to search for
     * @return true if that mapped field name is found
     */
    public MappedField getMappedField(final String storedName) {
        for (final MappedField mf : persistenceFields) {
            for (final String n : mf.getLoadNames()) {
                if (storedName.equals(n)) {
                    return mf;
                }
            }
        }

        return null;
    }

    /**
     * Returns MappedField for a given java field name on the this MappedClass
     *
     * @param name the Java field name to search for
     * @return the MappedField for the named Java field
     */
    public MappedField getMappedFieldByJavaField(final String name) {
        for (final MappedField mf : persistenceFields) {
            if (name.equals(mf.getJavaFieldName())) {
                return mf;
            }
        }

        return null;
    }

    /**
     * @return the ID field for the class
     */
    public MappedField getMappedIdField() {
        List<MappedField> fields = getFieldsAnnotatedWith(Id.class);
        return fields.isEmpty() ? null : fields.get(0);
    }

    /**
     * @return the ID field for the class
     */
    public MappedField getMappedVersionField() {
        List<MappedField> fields = getFieldsAnnotatedWith(Version.class);
        return fields.isEmpty() ? null : fields.get(0);
    }

    /**
     * @return the persistenceFields
     */
    public List<MappedField> getPersistenceFields() {
        return persistenceFields;
    }

    /**
     * @return the relevantAnnotations
     */
    public Map<Class<? extends Annotation>, List<Annotation>> getRelevantAnnotations() {
        return foundAnnotations;
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MappedClass that = (MappedClass) o;

        return clazz.equals(that.clazz);

    }

    boolean isSubType(final MappedClass mc) {
        return mc.equals(superClass) || interfaces.contains(mc);
    }

    @Override
    public String toString() {
        return "MappedClass - kind:" + getCollectionName() + " for " + getClazz().getName() + " fields:" + persistenceFields;
    }

    /**
     * Update mappings based on fields/annotations.
     */
    // TODO: Remove this and make these fields dynamic or auto-set some other way
    public void update() {
        embeddedAn = (Embedded) getAnnotation(Embedded.class);
        entityAn = (Entity) getFirstAnnotation(Entity.class);
        // polymorphicAn = (Polymorphic) getAnnotation(Polymorphic.class);
        final List<MappedField> fields = getFieldsAnnotatedWith(Id.class);
        if (fields != null && !fields.isEmpty()) {
            idField = fields.get(0).getField();
        }
    }

    /**
     * Validates this MappedClass
     * @param mapper the Mapper to use for validation
     */
    @SuppressWarnings("deprecation")
    public void validate(final Mapper mapper) {
        new MappingValidator(mapper.getOptions().getObjectFactory()).validate(mapper, this);
    }

    protected void basicValidate() {
        final boolean isStatic = Modifier.isStatic(clazz.getModifiers());
        if (!isStatic && clazz.isMemberClass()) {
            throw new MappingException("Cannot use non-static inner class: " + clazz + ". Please make static.");
        }
    }

    /**
     * Discovers interesting (that we care about) things about the class.
     */
    protected void discover(final Mapper mapper) {
        for (final Class<? extends Annotation> c : INTERESTING_ANNOTATIONS) {
            addAnnotation(c);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            superClass = mapper.getMappedClass(superclass);
        }
        for (Class<?> aClass : clazz.getInterfaces()) {
            interfaces.add(mapper.getMappedClass(aClass));
        }

        final List<Class<?>> lifecycleClasses = new ArrayList<Class<?>>();
        lifecycleClasses.add(clazz);

        final EntityListeners entityLisAnn = (EntityListeners) getAnnotation(EntityListeners.class);
        if (entityLisAnn != null && entityLisAnn.value().length != 0) {
            Collections.addAll(lifecycleClasses, entityLisAnn.value());
        }

        for (final Class<?> cls : lifecycleClasses) {
            for (final Method m : ReflectionUtils.getDeclaredAndInheritedMethods(cls)) {
                for (final Class<? extends Annotation> c : LIFECYCLE_ANNOTATIONS) {
                    if (m.isAnnotationPresent(c)) {
                        addLifecycleEventMethod(c, m, cls.equals(clazz) ? null : cls);
                    }
                }
            }
        }

        update();

        for (final java.lang.reflect.Field field : ReflectionUtils.getDeclaredAndInheritedFields(clazz, true)) {
            field.setAccessible(true);
            final int fieldMods = field.getModifiers();
            if (!isIgnorable(field, fieldMods, mapper)) {
                if (field.isAnnotationPresent(Id.class)) {
                    persistenceFields.add(new MappedField(field, clazz, mapper));
                    update();
                } else if (field.isAnnotationPresent(Property.class)
                           || field.isAnnotationPresent(Reference.class)
                           || field.isAnnotationPresent(Embedded.class)
                           || field.isAnnotationPresent(Serialized.class)
                           || isSupportedType(field.getType())
                           || ReflectionUtils.implementsInterface(field.getType(), Serializable.class)) {
                    persistenceFields.add(new MappedField(field, clazz, mapper));
                } else {
                    if (mapper.getOptions().getDefaultMapper() != null) {
                        persistenceFields.add(new MappedField(field, clazz, mapper));
                    } else if (LOG.isWarnEnabled()) {
                        LOG.warn(format("Ignoring (will not persist) field: %s.%s [type:%s]", clazz.getName(), field.getName(),
                                           field.getType().getName()));
                    }
                }
            }
        }
    }

    /**
     * Adds the annotation, if it exists on the field.
     */
    private void addAnnotation(final Class<? extends Annotation> clazz) {
        final List<? extends Annotation> annotations = ReflectionUtils.getAnnotations(getClazz(), clazz);
        for (final Annotation ann : annotations) {
            addAnnotation(clazz, ann);
        }
    }

    private void addLifecycleEventMethod(final Class<? extends Annotation> lceClazz, final Method m, final Class<?> clazz) {
        final ClassMethodPair cm = new ClassMethodPair(clazz, m);
        if (lifecycleMethods.containsKey(lceClazz)) {
            lifecycleMethods.get(lceClazz).add(cm);
        } else {
            final List<ClassMethodPair> methods = new ArrayList<ClassMethodPair>();
            methods.add(cm);
            lifecycleMethods.put(lceClazz, methods);
        }
    }

    private void callGlobalInterceptors(final Class<? extends Annotation> event, final Object entity, final DBObject dbObj,
                                        final Mapper mapper) {
        for (final EntityInterceptor ei : mapper.getInterceptors()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Calling interceptor method " + event.getSimpleName() + " on " + ei);
            }

            if (event.equals(PreLoad.class)) {
                ei.preLoad(entity, dbObj, mapper);
            } else if (event.equals(PostLoad.class)) {
                ei.postLoad(entity, dbObj, mapper);
            } else if (event.equals(PrePersist.class)) {
                ei.prePersist(entity, dbObj, mapper);
            } else if (event.equals(PreSave.class)) {
                ei.preSave(entity, dbObj, mapper);
            } else if (event.equals(PostPersist.class)) {
                ei.postPersist(entity, dbObj, mapper);
            }
        }
    }

    private Object getOrCreateInstance(final Class<?> clazz, final Mapper mapper) {
        if (mapper.getInstanceCache().containsKey(clazz)) {
            return mapper.getInstanceCache().get(clazz);
        }

        final Object o = mapper.getOptions().getObjectFactory().createInstance(clazz);
        final Object nullO = mapper.getInstanceCache().put(clazz, o);
        if (nullO != null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Race-condition, created duplicate class: " + clazz);
            }
        }

        return o;

    }

    private boolean isIgnorable(final java.lang.reflect.Field field, final int fieldMods, final Mapper mapper) {
        return field.isAnnotationPresent(Transient.class)
               || Modifier.isTransient(fieldMods)
               || field.isSynthetic() && Modifier.isTransient(fieldMods)
               || mapper.getOptions().isIgnoreFinals() && Modifier.isFinal(fieldMods);
    }

    private static class ClassMethodPair {
        private final Class<?> clazz;
        private final Method method;

        ClassMethodPair(final Class<?> c, final Method m) {
            clazz = c;
            method = m;
        }
    }

}
