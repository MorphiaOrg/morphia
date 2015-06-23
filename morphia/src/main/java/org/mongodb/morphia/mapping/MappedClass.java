package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.EntityListeners;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Polymorphic;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.validation.MappingValidator;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;


/**
 * Represents a mapped class between the MongoDB DBObject and the java POJO.
 * <p/>
 * This class will validate classes to make sure they meet the requirement for persistence.
 *
 * @author Scott Hernandez
 */
public class MappedClass {
    private static class ClassMethodPair {
        private final Class<?> clazz;
        private final Method method;

        public ClassMethodPair(final Class<?> c, final Method m) {
            clazz = c;
            method = m;
        }
    }

    private static final Logger LOG = MorphiaLoggerFactory.get(MappedClass.class);

    /**
     * Annotations we are interested in looking for.
     *
     * @see #addInterestingAnnotation
     */
    private static final List<Class<? extends Annotation>> INTERESTING_ANNOTATIONS = new ArrayList<Class<? extends Annotation>>();

    static {
        INTERESTING_ANNOTATIONS.add(Embedded.class);
        INTERESTING_ANNOTATIONS.add(Entity.class);
        INTERESTING_ANNOTATIONS.add(Polymorphic.class);
        INTERESTING_ANNOTATIONS.add(EntityListeners.class);
        INTERESTING_ANNOTATIONS.add(Version.class);
        INTERESTING_ANNOTATIONS.add(Converters.class);
        INTERESTING_ANNOTATIONS.add(Indexes.class);
        INTERESTING_ANNOTATIONS.add(Field.class);
        INTERESTING_ANNOTATIONS.add(IndexOptions.class);
    }

    /**
     * special fields representing the Key of the object
     */
    private java.lang.reflect.Field idField;

    /**
     * special annotations representing the type the object
     */
    private Entity entityAn;
    private Embedded embeddedAn;

    /**
     * Annotations interesting for life-cycle events
     */
    @SuppressWarnings("unchecked")
    private static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = asList(PrePersist.class,
                                                                                          PreSave.class,
                                                                                          PreLoad.class,
                                                                                          PostPersist.class,
                                                                                          PostLoad.class);
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

    private final Mapper mapper;

    public static void addInterestingAnnotation(final Class<? extends Annotation> annotation) {
        INTERESTING_ANNOTATIONS.add(annotation);
    }

    /**
     * constructor
     */
    public MappedClass(final Class<?> clazz, final Mapper mapper) {
        this.mapper = mapper;
        this.clazz = clazz;

        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating MappedClass for " + clazz);
        }

        basicValidate();
        discover();

        if (LOG.isDebugEnabled()) {
            LOG.debug("MappedClass done: " + toString());
        }
    }

    protected void basicValidate() {
        final boolean isStatic = Modifier.isStatic(clazz.getModifiers());
        if (!isStatic && clazz.isMemberClass()) {
            throw new MappingException("Cannot use non-static inner class: " + clazz + ". Please make static.");
        }
    }

    /*
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
     * Discovers interesting (that we care about) things about the class.
     */
    protected void discover() {
        for (final Class<? extends Annotation> c : INTERESTING_ANNOTATIONS) {
            addAnnotation(c);
        }

        final List<Class<?>> lifecycleClasses = new ArrayList<Class<?>>();
        lifecycleClasses.add(clazz);

        final EntityListeners entityLisAnn = (EntityListeners) getAnnotation(EntityListeners.class);
        if (entityLisAnn != null && entityLisAnn.value() != null && entityLisAnn.value().length != 0) {
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
            if (!isIgnorable(field, fieldMods)) {
                if (field.isAnnotationPresent(Id.class)) {
                    persistenceFields.add(new MappedField(field, clazz, getMapper()));
                    update();
                } else if (field.isAnnotationPresent(Property.class)
                           || field.isAnnotationPresent(Reference.class)
                           || field.isAnnotationPresent(Embedded.class)
                           || field.isAnnotationPresent(Serialized.class)
                           || isSupportedType(field.getType())
                           || ReflectionUtils.implementsInterface(field.getType(), Serializable.class)) {
                    persistenceFields.add(new MappedField(field, clazz, getMapper()));
                } else {
                    if (mapper.getOptions().getDefaultMapper() != null) {
                        persistenceFields.add(new MappedField(field, clazz, getMapper()));
                    } else if (LOG.isWarningEnabled()) {
                        LOG.warning(format("Ignoring (will not persist) field: %s.%s [type:%s]", clazz.getName(), field.getName(),
                                           field.getType().getName()));
                    }
                }
            }
        }
    }

    private boolean isIgnorable(final java.lang.reflect.Field field, final int fieldMods) {
        return field.isAnnotationPresent(Transient.class)
            || field.isSynthetic() && (fieldMods & Modifier.TRANSIENT) == Modifier.TRANSIENT
            || mapper.getOptions().isActLikeSerializer() && ((fieldMods & Modifier.TRANSIENT) == Modifier.TRANSIENT)
            || mapper.getOptions().isIgnoreFinals() && ((fieldMods & Modifier.FINAL) == Modifier.FINAL);
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

    /**
     * Adds the given Annotation to the internal list for the given Class.
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

    public List<ClassMethodPair> getLifecycleMethods(final Class<Annotation> clazz) {
        return lifecycleMethods.get(clazz);
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

    @Override
    public String toString() {
        return "MappedClass - kind:" + getCollectionName() + " for " + getClazz().getName() + " fields:" + persistenceFields;
    }

    /**
     * Returns fields annotated with the clazz
     */
    public List<MappedField> getFieldsAnnotatedWith(final Class<? extends Annotation> clazz) {
        final List<MappedField> results = new ArrayList<MappedField>();
        for (final MappedField mf : persistenceFields) {
            if (mf.getAnnotations().containsKey(clazz)) {
                results.add(mf);
            }
        }
        return results;
    }

    /**
     * Returns the MappedField by the name that it will stored in mongodb as
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
     * Check java field name that will stored in mongodb
     */
    public boolean containsJavaFieldName(final String name) {
        return getMappedField(name) != null;
    }

    /**
     * Returns MappedField for a given java field name on the this MappedClass
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
     * Checks to see if it a Map/Set/List or a property supported by the MongoDB java driver
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

    @SuppressWarnings("deprecation")
    public void validate() {
        new MappingValidator(mapper.getOptions().getObjectFactory()).validate(this);
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

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    /**
     * Call the lifecycle methods
     */
    @SuppressWarnings({"WMI", "unchecked"})
    public DBObject callLifecycleMethods(final Class<? extends Annotation> event, final Object entity, final DBObject dbObj) {
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
                        toCall.put(c, getOrCreateInstance(c));
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

            callGlobalInterceptors(event, entity, dbObj, mapper.getInterceptors());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return retDbObj;
    }

    private Object getOrCreateInstance(final Class<?> clazz) {
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

    private void callGlobalInterceptors(final Class<? extends Annotation> event, final Object entity, final DBObject dbObj,
                                        final Collection<EntityInterceptor> interceptors) {
        for (final EntityInterceptor ei : interceptors) {
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

    /**
     * @return the idField
     */
    public java.lang.reflect.Field getIdField() {
        return idField;
    }

    /**
     * @return the entityAn
     */
    public Entity getEntityAnnotation() {
        return entityAn;
    }

    /**
     * @return the embeddedAn
     */
    public Embedded getEmbeddedAnnotation() {
        return embeddedAn;
    }

    /**
     * @return the relevantAnnotations
     */
    public Map<Class<? extends Annotation>, List<Annotation>> getRelevantAnnotations() {
        return foundAnnotations;
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
     * @return the instance if it was found, if more than one was found, the last one added
     */
    public Annotation getAnnotation(final Class<? extends Annotation> clazz) {
        final List<Annotation> found = foundAnnotations.get(clazz);
        return found == null || found.isEmpty() ? null : found.get(found.size() - 1);
    }

    /**
     * @return the instance if it was found, if more than one was found, the last one added
     */
    public List<Annotation> getAnnotations(final Class<? extends Annotation> clazz) {
        return foundAnnotations.get(clazz);
    }

    /**
     * @return the persistenceFields
     */
    public List<MappedField> getPersistenceFields() {
        return persistenceFields;
    }

    /**
     * @return the collName
     */
    public String getCollectionName() {
        if (entityAn == null || entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
            return mapper.getOptions().isUseLowerCaseCollectionNames() ? clazz.getSimpleName().toLowerCase() : clazz.getSimpleName();
        }
        return entityAn.value();
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * @return the Mapper this class is bound to
     */
    public Mapper getMapper() {
        return mapper;
    }

    public MappedField getMappedIdField() {
        List<MappedField> fields = getFieldsAnnotatedWith(Id.class);
        return fields.isEmpty() ? null : fields.get(0);
    }

}