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

package dev.morphia.mapping.lazy;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;

import java.io.Serializable;

public class TransientMappingTest extends TestBase {

    @Test
    public void mapClassWithTransientFields() {
        getMorphia().map(HasTransientFields.class);
        final HasTransientFields entity = new HasTransientFields();
        entity.javaTransientString = "should not be persisted";
        entity.morphiaTransientString = "should not be persisted";
        entity.javaTransientInt = -1;
        entity.morphiaTransientInt = -1;

        final SerializableClass serializable = new SerializableClass();
        serializable.value = "foo";
        entity.javaTransientSerializable = serializable;
        entity.morphiaTransientSerializable = serializable;

        getDs().save(entity);
        DBObject dbObj = getDs().getCollection(HasTransientFields.class).findOne();
        Assert.assertFalse("morphiaTransientString", dbObj.containsField("morphiaTransientString"));
        Assert.assertFalse("morphiaTransientInt", dbObj.containsField("morphiaTransientInt"));
        Assert.assertFalse("morphiaTransientSerializable", dbObj.containsField("morphiaTransientSerializable"));
        Assert.assertFalse("javaTransientString", dbObj.containsField("javaTransientString"));
        Assert.assertFalse("javaTransientInt", dbObj.containsField("javaTransientInt"));
        Assert.assertFalse("javaTransientSerializable", dbObj.containsField("javaTransientSerializable"));
    }

    private static class HasTransientFields {
        @Id
        private ObjectId id;

        private transient String javaTransientString;
        private transient int javaTransientInt;
        private transient SerializableClass javaTransientSerializable;
        @Transient
        private String morphiaTransientString;
        @Transient
        private int morphiaTransientInt;
        @Transient
        private SerializableClass morphiaTransientSerializable;

    }

    private static class SerializableClass implements Serializable {
        private String value;
    }
}
