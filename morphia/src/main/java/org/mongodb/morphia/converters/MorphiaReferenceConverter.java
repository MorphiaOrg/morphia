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

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.mongodb.morphia.MorphiaReference;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

public class MorphiaReferenceConverter extends TypeConverter implements SimpleValueConverter {

    protected MorphiaReferenceConverter() {
        super(MorphiaReference.class);
    }

    @Override
    protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
        return super.isSupported(c, optionalExtraInfo);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        return value instanceof MorphiaReference ? ((MorphiaReference) value).getDBRef() : value;

    }

    @Override
    public Object decode(final Class<?> targetClass, final Object fromDBObject, final MappedField mappedField) {
        if (fromDBObject instanceof DBRef) {
            DBRef dbRef = (DBRef) fromDBObject;
            return new MorphiaReference<Object>(dbRef.getId(), dbRef.getCollectionName(), null);
        } else {
            Class refClass;
            if (!mappedField.getType().equals(MorphiaReference.class)) {
                refClass = mappedField.getTypeParameters().get(0).getSubClass();
            } else {
                refClass = mappedField.getTypeParameters().get(0).getType();
            }
            MappedClass mappedClass = getMapper().getMappedClass(refClass);
            if (fromDBObject instanceof DBObject) {
                ((DBObject) fromDBObject).removeField(Mapper.CLASS_NAME_FIELDNAME);
            }
            return new MorphiaReference<Object>(fromDBObject, mappedClass.getCollectionName(), null);
        }
    }
}
