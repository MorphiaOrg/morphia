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

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.MappingValidator;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
public class MappedClass {
    private static final Logger LOG = LoggerFactory.getLogger(MappedClass.class);

    /**
     * a list of the fields to map
     */
    private final List<MappedField> fields = new ArrayList<>();
    /**
     * the type we are mapping to/from
     */
    private final EntityModel<?> entityModel;
    private final Class<?> type;
    /**
     * special fields representing the Key of the object
     */
    private MappedField idField;
    private MappedClass superClass;
    private List<MappedClass> interfaces = new ArrayList<>();
    private List<MappedClass> subtypes = new ArrayList<>();

    /**
     * Creates a MappedClass instance
     *
     * @param entityModel the ClassModel
     * @param mapper       the Mapper to use
     */
    public MappedClass(final EntityModel entityModel, final Mapper mapper) {
        this.entityModel = entityModel;
        type = entityModel.getType();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating MappedClass for " + type);
        }

        if (!Modifier.isStatic(type.getModifiers()) && type.isMemberClass()) {
            throw new MappingException(format("Cannot use non-static inner class: %s. Please make static.", type));
        }
        discover(mapper);

        if (LOG.isDebugEnabled()) {
            LOG.debug("MappedClass done: " + this);
        }
    }

    /**
     * @return the MappedClasses for all the known subtypes
     */
    public List<MappedClass> getSubtypes() {
        return Collections.unmodifiableList(subtypes);
    }

    /**
     * Update mappings based on fields/annotations.
     */
    public void update() {
        final List<MappedField> fields = getFields(Id.class);
        if (fields != null && !fields.isEmpty()) {
            idField = fields.get(0);
        }
    }

    /**
     * Returns fields annotated with the clazz
     *
     * @param clazz The Annotation to find.
     * @return the list of fields
     */
    public List<MappedField> getFields(final Class<? extends Annotation> clazz) {
        final List<MappedField> results = new ArrayList<>();
        for (final MappedField mf : fields) {
            if (mf.hasAnnotation(clazz)) {
                results.add(mf);
            }
        }
        return results;
    }

    /**
     * This is an internal method subject to change without notice.
     *
     * @return the parent class of this type if there is one null otherwise
     * @since 1.3
     */
    public MappedClass getSuperClass() {
        return superClass;
    }

    /**
     * @return true if the MappedClass is an interface
     */
    public boolean isInterface() {
        return type.isInterface();
    }

    /**
     * This is an internal method subject to change without notice.
     *
     * @return true if the MappedClass is abstract
     * @since 1.3
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(type.getModifiers());
    }

    /**
     * Call the lifecycle methods
     *
     * @param event    the lifecycle annotation
     * @param entity   the entity to process
     * @param document the document to use
     * @param mapper   the Mapper to use
     */
    public void callLifecycleMethods(final Class<? extends Annotation> event, final Object entity, final Document document,
                                     final Mapper mapper) {
        entityModel.callLifecycleMethods(event, entity, document, mapper);
    }

    /**
     * Checks if this mapped type has the given lifecycle event defined
     *
     * @param type the event type
     * @return true if this annotation has been found
     */
    public boolean hasLifecycle(final Class<? extends Annotation> type) {
        return entityModel.hasLifecycle(type);
    }

    /**
     * Looks for an annotation of the type given
     *
     * @param clazz the type to search for
     * @param <T>   the annotation type
     * @return the instance if it was found, if more than one was found, the last one added
     */
    public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
        return entityModel.getAnnotation(clazz);
    }

    /**
     * Looks for an annotation in the annotations found on a class while mapping
     *
     * @param clazz the class to search for
     * @param <T>   the type of annotation to find
     * @return the instance if it was found, if more than one was found, the last one added
     */
    public <T extends Annotation> List<T> getAnnotations(final Class<T> clazz) {
        return entityModel.getAnnotations(clazz);
    }

    /**
     * @return the embeddedAn
     */
    public Embedded getEmbeddedAnnotation() {
        return entityModel.getAnnotation(Embedded.class);
    }

    /**
     * @return the entityAn
     */
    public Entity getEntityAnnotation() {
        return entityModel.getAnnotation(Entity.class);
    }

    /**
     * @return the idField
     */
    public MappedField getIdField() {
        return idField;
    }

    /**
     * Returns the MappedField by the name that it will stored in mongodb as
     *
     * @param storedName the name to search for
     * @return true if that mapped field name is found
     */
    public MappedField getMappedField(final String storedName) {
        return fields.stream()
                     .filter(mappedField -> mappedField.getMappedFieldName().equals(storedName)
                                            || mappedField.getJavaFieldName().equals(storedName))
                     .findFirst()
                     .orElse(null);
    }

    /**
     * Returns MappedField for a given java field name on the this MappedClass
     *
     * @param name the Java field name to search for
     * @return the MappedField for the named Java field
     */
    public MappedField getMappedFieldByJavaField(final String name) {
        for (final MappedField mf : fields) {
            if (name.equals(mf.getJavaFieldName())) {
                return mf;
            }
        }

        return null;
    }

    /**
     * @return the ID field for the class
     */
    public MappedField getVersionField() {
        List<MappedField> fields = getFields(Version.class);
        return fields.isEmpty() ? null : fields.get(0);
    }

    /**
     * @return the fields
     */
    public List<MappedField> getFields() {
        return fields;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
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

        return type.equals(that.type);

    }

    @Override
    public String toString() {
        return format("%s[%s]", getType().getSimpleName(), getCollectionName());
    }

    /**
     * @return the clazz
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return the collName
     */
    public String getCollectionName() {
        return entityModel.getCollectionName();
    }

    /**
     * Validates this MappedClass
     *
     * @param mapper the Mapper to use for validation
     */
    public void validate(final Mapper mapper) {
        MorphiaInstanceCreator factory = (MorphiaInstanceCreator) entityModel.getInstanceCreatorFactory()
                                                                             .create();
        new MappingValidator(factory).validate(mapper, this);
    }

    /**
     * @return the underlying model of the type
     */
    public EntityModel<?> getEntityModel() {
        return entityModel;
    }

    /**
     * Discovers interesting (that we care about) things about the class.
     */
    private void discover(final Mapper mapper) {
        Class<?> superclass = type.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            superClass = mapper.getMappedClass(superclass);
            if (superClass != null) {
                superClass.addSubtype(this);
            }
        }

        for (Class<?> aClass : type.getInterfaces()) {
            final MappedClass mappedClass = mapper.getMappedClass(aClass);
            if (mappedClass != null) {
                mappedClass.addSubtype(this);
                this.interfaces.add(mappedClass);
            }
        }

        discoverFields();

        update();
    }

    private void addSubtype(final MappedClass mappedClass) {
        subtypes.add(mappedClass);
    }

    private void discoverFields() {
        entityModel.getFieldModels().forEach(model -> {
            final MappedField field = new MappedField(this, model);
            if (!field.isTransient()) {
                fields.add(field);
            } else {
                Sofia.logIgnoringTransientField(field.getFullName());
            }
        });
    }

    boolean isSubType(final MappedClass mc) {
        return mc.equals(superClass) || interfaces.contains(mc);
    }

}
