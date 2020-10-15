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

import com.mongodb.DBRef;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.codec.pojo.FieldModel;
import morphia.org.bson.codecs.pojo.TypeData;
import org.bson.Document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class MappedField {
    private final MappedClass declaringClass;
    private final FieldModel<?> fieldModel;


    MappedField(MappedClass declaringClass, FieldModel<?> f) {
        this.declaringClass = declaringClass;
        fieldModel = f;
    }

    /**
     * @return the name of the field's (key)name for mongodb
     */
    public String getMappedFieldName() {
        return fieldModel.getMappedName();
    }

    /**
     * @param clazz the annotation to search for
     * @param <T>   the type of the annotation
     * @return the annotation instance if it exists on this field
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> clazz) {
        return fieldModel.getAnnotation(clazz);
    }

    /**
     * @return the declaring class of the java field
     */
    public MappedClass getDeclaringClass() {
        return declaringClass;
    }

    /**
     * @return the underlying java field
     */
    public Field getField() {
        return fieldModel.getField();
    }

    /**
     * @param document the Document get the value from
     * @return the value from first mapping of this field
     */
    public Object getDocumentValue(Document document) {
        return fieldModel.getDocumentValue(document);
    }

    /**
     * Gets the value of the field mapped on the instance given.
     *
     * @param instance the instance to use
     * @return the value stored in the java field
     */
    public Object getFieldValue(Object instance) {
        return fieldModel.getValue(instance);
    }

    /**
     * @return the full name of the class plus java field name
     */
    public String getFullName() {
        return fieldModel.getFullName();
    }

    /**
     * @return the name of the field's (key)name for mongodb, in order of loading.
     */
    public List<String> getLoadNames() {
        return fieldModel.getLoadNames();
    }

    /**
     * @return the name of the java field, as declared on the class
     */
    public String getJavaFieldName() {
        return fieldModel.getName();
    }

    /**
     * If the java field is a list/array/map then the sub-type T is returned (ex. List&lt;T&gt;, T[], Map&lt;?,T&gt;)
     *
     * @return the parameterized type of the field
     */
    public Class<?> getSpecializedType() {
        return fieldModel.getSpecializedType();
    }

    /**
     * @return the type of the underlying java field
     */
    public Class<?> getType() {
        return fieldModel.getTypeData().getType();
    }

    /**
     * Indicates whether the annotation is present in the mapping (does not check the java field annotations, just the ones discovered)
     *
     * @param ann the annotation to search for
     * @return true if the annotation was found
     */
    public boolean hasAnnotation(Class<? extends Annotation> ann) {
        return fieldModel.hasAnnotation(ann);
    }

    /**
     * @return true if the MappedField is an array
     */
    public boolean isArray() {
        return fieldModel.isArray();
    }

    /**
     * @return true if the MappedField is a Map
     */
    public boolean isMap() {
        return fieldModel.isMap();
    }

    /**
     * @return true if this field is a container type such as a List, Map, Set, or array
     */
    public boolean isMultipleValues() {
        return fieldModel.isMultipleValues();
    }

    /**
     * @return true if this field is a reference to a foreign document
     * @see Reference
     * @see Key
     * @see DBRef
     */
    public boolean isReference() {
        return fieldModel.isReference();
    }

    /**
     * @return the field's type data
     */
    public TypeData<?> getTypeData() {
        return fieldModel.getTypeData();
    }

    /**
     * @return true if this field is not a container type such as a List, Map, Set, or array
     */
    public boolean isScalarValue() {
        return fieldModel.isScalarValue();
    }

    /**
     * @return true if the MappedField is a Set
     */
    public boolean isSet() {
        return fieldModel.isSet();
    }

    /**
     * @return true if this field is marked as transient
     */
    public boolean isTransient() {
        return fieldModel.isTransient();
    }

    /**
     * Sets the value for the java field
     *
     * @param instance the instance to update
     * @param value    the value to set
     */
    public void setFieldValue(Object instance, Object value) {
        fieldModel.setFieldValue(instance, value);
    }

    @Override
    public String toString() {
        return format("%s : %s", getMappedFieldName(), fieldModel.getTypeData().toString());
    }

    /**
     * Gets the parameterized type of a List or the key type of a Map, e.g.
     *
     * @return the unwrapped type
     */
    public Class getNormalizedType() {
        return fieldModel.getNormalizedType();
    }

    /**
     * @return the underlying field model
     */
    public FieldModel getFieldModel() {
        return fieldModel;
    }
}
