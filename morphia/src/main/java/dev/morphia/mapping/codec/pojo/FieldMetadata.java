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

import morphia.org.bson.codecs.pojo.TypeData;
import morphia.org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Contains various metadata about a field
 *
 * @param <T> the field type
 * @morphia.internal
 * @since 2.0
 */
public final class FieldMetadata<T> {
    private final String name;
    private final TypeData<T> typeData;
    private final List<Annotation> annotations = new ArrayList<>();
    private TypeParameterMap typeParameterMap;
    private List<TypeData<?>> typeParameters;
    private Field field;

    /**
     * Createss an instance for the field
     *
     * @param field               the field
     * @param typeData            the type data
     * @param typeParameterMap    the type parameter map
     * @param parentClassTypeData the parent class type data
     */
    public FieldMetadata(final Field field,
                         final TypeData<T> typeData,
                         final TypeParameterMap typeParameterMap,
                         final TypeData<?> parentClassTypeData) {
        this.field = field;
        this.name = field.getName();
        this.typeData = typeData;

        if (typeParameterMap != null && parentClassTypeData != null) {
            this.typeParameterMap = typeParameterMap;
            this.typeParameters = parentClassTypeData.getTypeParameters();
        }
    }

    /**
     * Adds an annotation to the metadata
     *
     * @param annotation the annotation
     * @return this
     */
    public FieldMetadata<T> addAnnotation(final Annotation annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * @return any annotations defined on the field
     */
    public List<Annotation> getAnnotations() {
        return unmodifiableList(annotations);
    }

    /**
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type data
     */
    public TypeData<T> getTypeData() {
        return typeData;
    }

    /**
     * @return the type parameter map
     */
    public TypeParameterMap getTypeParameterMap() {
        return typeParameterMap;
    }

    /**
     * @return the type parameters
     */
    public List<TypeData<?>> getTypeParameters() {
        return typeParameters;
    }

    /**
     * Sets the type parameter information
     *
     * @param typeParameterMap the map to use
     * @param parentTypeData   the parent's type data
     * @param <S>              the parent's type
     * @return this
     */
    public <S> FieldMetadata<T> typeParameterInfo(final TypeParameterMap typeParameterMap, final TypeData<S> parentTypeData) {
        if (typeParameterMap != null && parentTypeData != null) {
            this.typeParameterMap = typeParameterMap;
            this.typeParameters = parentTypeData.getTypeParameters();
        }
        return this;
    }

}
