/*
 * Copyright 2016 MongoDB, Inc.
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

package org.mongodb.morphia;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestArrayUpdates.Grade;
import org.mongodb.morphia.TestArrayUpdates.Student;
import org.mongodb.morphia.entities.EmbeddedSubtype;
import org.mongodb.morphia.entities.EmbeddedType;
import org.mongodb.morphia.entities.EntityWithListsAndArrays;
import org.mongodb.morphia.entities.ParentType;
import org.mongodb.morphia.mapping.EmbeddedMappingTest.AnotherNested;
import org.mongodb.morphia.mapping.EmbeddedMappingTest.Nested;
import org.mongodb.morphia.mapping.EmbeddedMappingTest.NestedImpl;
import org.mongodb.morphia.mapping.EmbeddedMappingTest.WithNested;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;

public class PathTargetTest extends TestBase {

    @Test
    public void simpleResolution() {
        getMorphia().map(ParentType.class, EmbeddedType.class);
        Mapper mapper = getMorphia().getMapper();
        MappedClass mappedClass = mapper.getMappedClass(ParentType.class);

        PathTarget pathTarget = new PathTarget(mapper, mappedClass, "name");
        Assert.assertEquals("n", pathTarget.translatedPath());
        Assert.assertEquals(mappedClass.getMappedFieldByJavaField("name"), pathTarget.getTarget());

        pathTarget = new PathTarget(mapper, mappedClass, "n");
        Assert.assertEquals("n", pathTarget.translatedPath());
        Assert.assertEquals(mappedClass.getMappedField("n"), pathTarget.getTarget());
    }

    @Test
    public void dottedPath() {
        getMorphia().map(ParentType.class, EmbeddedType.class);
        Mapper mapper = getMorphia().getMapper();

        PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(ParentType.class), "embedded.number");
        Assert.assertEquals("embedded.number", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getMappedClass(EmbeddedType.class).getMappedFieldByJavaField("number"), pathTarget.getTarget());
    }

    @Test
    public void subClasses() {
        getMorphia().map(ParentType.class, EmbeddedType.class, EmbeddedSubtype.class);
        Mapper mapper = getMorphia().getMapper();

        PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(ParentType.class), "embedded.flag");
        Assert.assertEquals("embedded.flag", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getMappedClass(EmbeddedSubtype.class).getMappedFieldByJavaField("flag"), pathTarget.getTarget());
    }

    @Test
    public void arrays() {
        getMorphia().map(EntityWithListsAndArrays.class, EmbeddedType.class, Student.class);
        Mapper mapper = getMorphia().getMapper();
        MappedClass mappedClass = mapper.getMappedClass(EntityWithListsAndArrays.class);

        PathTarget pathTarget = new PathTarget(mapper, mappedClass, "listEmbeddedType.1.number");
        Assert.assertEquals("listEmbeddedType.1.number", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getMappedClass(EmbeddedType.class).getMappedFieldByJavaField("number"), pathTarget.getTarget());
    }

    @Test
    public void maps() {
        getMorphia().map(Student.class);
        Mapper mapper = getMorphia().getMapper();
        MappedClass mappedClass = mapper.getMappedClass(Student.class);

        final PathTarget pathTarget = new PathTarget(mapper, mappedClass, "grades.$.data.name");
        Assert.assertEquals("grades.$.data.name", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getMappedClass(Grade.class).getMappedFieldByJavaField("data"), pathTarget.getTarget());
    }

    @Test
    public void interfaces() {
        getMorphia().map(WithNested.class, Nested.class, NestedImpl.class, AnotherNested.class);
        Mapper mapper = getMorphia().getMapper();
        MappedClass mappedClass = mapper.getMappedClass(WithNested.class);

        PathTarget pathTarget = new PathTarget(mapper, mappedClass, "nested.value");
        Assert.assertEquals("nested.value", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getMappedClass(AnotherNested.class).getMappedFieldByJavaField("value"), pathTarget.getTarget());

        pathTarget = new PathTarget(mapper, mappedClass, "nested.field");
        Assert.assertEquals("nested.field", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getMappedClass(NestedImpl.class).getMappedFieldByJavaField("field"), pathTarget.getTarget());
    }

    @Test
    public void disableValidation() {
        getMorphia().map(WithNested.class, Nested.class, NestedImpl.class, AnotherNested.class);
        Mapper mapper = getMorphia().getMapper();
        MappedClass mappedClass = mapper.getMappedClass(WithNested.class);

        final PathTarget pathTarget = new PathTarget(mapper, mappedClass, "nested.field.fail");
        pathTarget.disableValidation();
        Assert.assertEquals("nested.field.fail", pathTarget.translatedPath());
        Assert.assertNull(pathTarget.getTarget());
    }
}
