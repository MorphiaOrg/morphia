/*
 * Copyright 2017 MongoDB, Inc.
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

package xyz.morphia.converters;

import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.mapping.experimental.MorphiaReference;

public class ReferenceConverter extends TypeConverter {
    private Mapper mapper;

    /**
     * Creates the Converter.
     * @param mapper
     */
    public ReferenceConverter(final Mapper mapper) {
        super(MorphiaReference.class);
        this.mapper = mapper;
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        MorphiaReference reference = (MorphiaReference) value;
        if(reference.isResolved()) {
            final Object wrapped = reference.get();
            Object id = reference.getId();
            if(id == null) {
                id = mapper.getId(wrapped);
            }
            return mapper.toMongoObject(optionalExtraInfo, mapper.getMappedClass(wrapped), id);
        } else {
            return null;
        }
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object idValue, final MappedField optionalExtraInfo) {
        return idValue == null ? null : MorphiaReference.wrapId(idValue);
    }
}
