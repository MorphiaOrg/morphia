/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package org.mongodb.morphia.converters;

import com.mongodb.DBRef;
import org.mongodb.morphia.MorphiaReference;
import org.mongodb.morphia.mapping.MappedField;

public class MorphiaReferenceConverter extends TypeConverter implements SimpleValueConverter {

    protected MorphiaReferenceConverter() {
        super(MorphiaReference.class, DBRef.class);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        //throw new UnsupportedOperationException("Not implemented yet!");
        return value instanceof MorphiaReference ? ((MorphiaReference) value).getDBRef() : value;

    }

    @Override
    public Object decode(final Class<?> targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
        DBRef dbRef = (DBRef) fromDBObject;
        return new MorphiaReference<Object>(dbRef.getId(), dbRef.getCollectionName(), null);
    }
}
