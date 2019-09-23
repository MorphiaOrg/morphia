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

import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.MorphiaModel;
import dev.morphia.mapping.validation.MappingValidator;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.pojo.ClassModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
    private final MorphiaModel<?> morphiaModel;
    private final Class<?> type;
    /**
     * special fields representing the Key of the object
     */
    private MappedField idField;
    private MappedClass superClass;
    private List<MappedClass> interfaces = new ArrayList<>();

    /**
     * Creates a MappedClass instance
     *
     * @param morphiaModel the ClassModel
     * @param mapper     the Mapper to use
     */
    public MappedClass(final MorphiaModel morphiaModel, final Mapper mapper) {
        this.morphiaModel = morphiaModel;
        type = morphiaModel.getType();

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
        morphiaModel.callLifecycleMethods(event, entity, document, mapper);
    }

    public boolean hasLifecycle(Class<? extends Annotation> klass) {
        return morphiaModel.hasLifecycle(klass);
    }

    /**
     * Looks for an annotation of the type given
     *
     * @param clazz the type to search for
     * @return the instance if it was found, if more than one was found, the last one added
     */
    public <T> T getAnnotation(final Class<? extends Annotation> clazz) {
        return morphiaModel.getAnnotation(clazz);
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
        return morphiaModel.getAnnotations(clazz);
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
       return morphiaModel.getCollectionName();
    }

    /**
     * @return the embeddedAn
     */
    public Embedded getEmbeddedAnnotation() {
        return morphiaModel.getAnnotation(Embedded.class);
    }

    /**
     * @return the entityAn
     */
    public Entity getEntityAnnotation() {
        return morphiaModel.getAnnotation(Entity.class);
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
                     .filter(mappedField -> mappedField.getMappedFieldName().equals(storedName) ||
                         mappedField.getJavaFieldName().equals(storedName))
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

    boolean isSubType(final MappedClass mc) {
        return mc.equals(superClass) || interfaces.contains(mc);
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
     * Validates this MappedClass
     *
     * @param mapper the Mapper to use for validation
     */
    public void validate(final Mapper mapper) {
        MorphiaInstanceCreator factory = (MorphiaInstanceCreator) morphiaModel.getInstanceCreatorFactory()
                                                                              .create();
        new MappingValidator(factory).validate(mapper, this);
    }

    /**
     * Discovers interesting (that we care about) things about the class.
     */
    private void discover(final Mapper mapper) {
        Class<?> superclass = type.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            superClass = mapper.getMappedClass(superclass);
        }

        for (Class<?> aClass : type.getInterfaces()) {
            final MappedClass mappedClass = mapper.getMappedClass(aClass);
            if (mappedClass != null) {
                this.interfaces.add(mappedClass);
            }
        }

        discoverFields();

        update();
    }

    private void discoverFields() {
        morphiaModel.getFieldModels().forEach(model -> {
            final MappedField field = new MappedField(this, model);
            if (!field.isTransient()) {
                fields.add(field);
            } else {
                Sofia.logIgnoringTransientField(field.getFullName());
            }
        });
    }

    public MorphiaModel<?> getMorphiaModel() {
        return morphiaModel;
    }

}
