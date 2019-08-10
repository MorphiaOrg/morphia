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

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.TypeData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.bson.assertions.Assertions.notNull;

/**
 * A builder for programmatically creating {@code FieldModels}.
 *
 * @param <T> the type of the field
 */
public final class FieldModelBuilder<T> {
    private Field field;
    private String name;
    private String readName;
    private String writeName;
    private TypeData<T> typeData;
    private Codec<T> codec;
    private List<Annotation> annotations = emptyList();

    FieldModelBuilder() {
    }

    /**
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the name of the field to use as the key when deserializing the data from BSON.
     */
    public String getReadName() {
        return readName;
    }

    /**
     * Sets the readName, the key for this field when deserializing the data from BSON.
     *
     * <p>Note: A null means this field will not used when deserializing.</p>
     *
     * @param readName the name of the field to use as the key when deserializing the data from BSON.
     * @return this
     */
    public FieldModelBuilder<T> readName(final String readName) {
        this.readName = readName;
        return this;
    }

    /**
     * @return the name of the field to use as the key when serializing the data into BSON.
     */
    public String getWriteName() {
        return writeName;
    }

    /**
     * Sets the writeName, the key for this field when serializing the data into BSON.
     *
     * <p>Note: A null means this field will not be serialized.</p>
     *
     * @param writeName the name of the field to use as the key when serializing the data into BSON.
     * @return this
     */
    public FieldModelBuilder<T> writeName(final String writeName) {
        this.writeName = writeName;
        return this;
    }

    /**
     * Sets a custom codec for the field
     *
     * @param codec the custom codec for the field
     * @return this
     */
    public FieldModelBuilder<T> codec(final Codec<T> codec) {
        this.codec = codec;
        return this;
    }

    /**
     * @return the custom codec to use if set or null
     */
    Codec<T> getCodec() {
        return codec;
    }

    /**
     * Returns the read annotations,  to be applied when serializing to BSON
     *
     * @return the read annotations
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public <A extends Annotation> A getAnnotation(Class<A> klass) {
        for (Annotation annotation : annotations) {
            if (klass.equals(annotation.annotationType())) {
                return klass.cast(annotation);
            }
        }
        return null;
    }

    public boolean hasAnnotation(Class<? extends Annotation> klass) {
        for (Annotation annotation : annotations) {
            if (klass.equals(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the annotations
     *
     * @param annotations the annotations
     * @return this
     */
    public FieldModelBuilder<T> annotations(final List<Annotation> annotations) {
        this.annotations = unmodifiableList(notNull("annotations", annotations));
        return this;
    }

    //    /**
    //     * Returns the {@link FieldAccessor}
    //     *
    //     * @return the FieldAccessor
    //     */
    //    public FieldAccessor<T> getFieldAccessor() {
    //        return fieldAccessor;
    //    }
    //
    //    /**
    //     * Sets the {@link FieldAccessor}
    //     *
    //     * @param fieldAccessor the FieldAccessor
    //     * @return this
    //     */
    //    public FieldModelBuilder<T> fieldAccessor(final FieldAccessor<T> fieldAccessor) {
    //        this.fieldAccessor = fieldAccessor;
    //        return this;
    //    }
    //

    /**
     * Creates the {@link FieldModel}.
     *
     * @return the FieldModel
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public FieldModel<T> build() {
        return new FieldModel(field, name, readName, writeName, typeData, annotations, codec);
    }

    @Override
    public String toString() {
        return format("FieldModelBuilder{fieldName=%s, typeData=%s}", name, typeData);
    }

    public FieldModelBuilder<T> field(final Field field) {
        this.field = notNull("field", field);
        return this;
    }

    public Field getField() {
        return field;
    }

    public FieldModelBuilder<T> fieldName(final String fieldName) {
        this.name = notNull("fieldName", fieldName);
        return this;
    }

    public String getFieldName() {
        return this.name;
    }

    public TypeData<T> getTypeData() {
        return typeData;
    }

    public FieldModelBuilder<T> typeData(final TypeData<T> typeData) {
        this.typeData = notNull("typeData", typeData);
        return this;
    }
}
