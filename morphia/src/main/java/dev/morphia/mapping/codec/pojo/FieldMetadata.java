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

import org.bson.codecs.pojo.TypeData;
import org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class FieldMetadata<T> {
    private final String name;
    private final TypeData<T> typeData;
    private final List<Annotation> annotations = new ArrayList<Annotation>();
    private TypeParameterMap typeParameterMap;
    private List<TypeData<?>> typeParameters;

    private Field field;

    public FieldMetadata(final Field field, final TypeData<T> typeData) {
        this.field = field;
        this.name = field.getName();
        this.typeData = typeData;
    }

    public String getName() {
        return name;
    }

    public List<Annotation> getAnnotations() {
        return unmodifiableList(annotations);
    }

    public FieldMetadata<T> addAnnotation(final Annotation annotation) {
        annotations.add(annotation);
        return this;
    }

    public Field getField() {
        return field;
    }

    public FieldMetadata<T> field(final Field field) {
        this.field = field;
        return this;
    }

    public TypeData<T> getTypeData() {
        return typeData;
    }

    public TypeParameterMap getTypeParameterMap() {
        return typeParameterMap;
    }

    public List<TypeData<?>> getTypeParameters() {
        return typeParameters;
    }

    public <S> FieldMetadata<T> typeParameterInfo(final TypeParameterMap typeParameterMap, final TypeData<S> parentTypeData) {
        if (typeParameterMap != null && parentTypeData != null) {
            this.typeParameterMap = typeParameterMap;
            this.typeParameters = parentTypeData.getTypeParameters();
        }
        return this;
    }
}
