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

package dev.morphia.internal;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.entities.EmbeddedSubtype;
import dev.morphia.entities.EmbeddedType;
import dev.morphia.entities.EntityWithListsAndArrays;
import dev.morphia.entities.ParentType;
import dev.morphia.mapping.EmbeddedMappingTest.AnotherNested;
import dev.morphia.mapping.EmbeddedMappingTest.Nested;
import dev.morphia.mapping.EmbeddedMappingTest.NestedImpl;
import dev.morphia.mapping.EmbeddedMappingTest.WithNested;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.testmodel.Article;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class PathTargetTest extends TestBase {

    @Test
    public void arrays() {
        getMapper().map(EntityWithListsAndArrays.class, EmbeddedType.class);
        Mapper mapper = getMapper();
        EntityModel entityModel = mapper.getEntityModel(EntityWithListsAndArrays.class);

        PathTarget pathTarget = new PathTarget(mapper, entityModel, "listEmbeddedType.1.number");
        Assert.assertEquals("listEmbeddedType.1.number", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(EmbeddedType.class).getField("number"), pathTarget.getTarget());

        assertEquals("listEmbeddedType.$", new PathTarget(mapper, entityModel, "listEmbeddedType.$").translatedPath());
        assertEquals("listEmbeddedType.1", new PathTarget(mapper, entityModel, "listEmbeddedType.1").translatedPath());
    }

    @Test
    public void dottedPath() {
        getMapper().map(ParentType.class, EmbeddedType.class);
        Mapper mapper = getMapper();

        PathTarget pathTarget = new PathTarget(mapper, ParentType.class, "embedded.number");
        Assert.assertEquals("embedded.number", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(EmbeddedType.class).getField("number"), pathTarget.getTarget());
    }

    @Test
    public void interfaces() {
        getMapper().map(NestedImpl.class, WithNested.class, Nested.class, AnotherNested.class);
        Mapper mapper = getMapper();
        EntityModel entityModel = mapper.getEntityModel(WithNested.class);

        PathTarget pathTarget = new PathTarget(mapper, entityModel, "nested.value");
        Assert.assertEquals("nested.value", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(AnotherNested.class).getField("value"), pathTarget.getTarget());

        pathTarget = new PathTarget(mapper, entityModel, "nested.field");
        Assert.assertEquals("nested.field", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(NestedImpl.class).getField("field"), pathTarget.getTarget());
    }

    @Test
    @Category(Reference.class)
    public void maps() {
        getMapper().map(Student.class, Article.class);
        Mapper mapper = getMapper();
        EntityModel entityModel = mapper.getEntityModel(Student.class);

        PathTarget pathTarget = new PathTarget(mapper, entityModel, "grades.$.data.name");
        Assert.assertEquals("grades.$.d.name", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(Grade.class).getField("data"), pathTarget.getTarget());

        pathTarget = new PathTarget(mapper, entityModel, "grades.$.d.name");
        Assert.assertEquals("grades.$.d.name", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(Grade.class).getField("d"), pathTarget.getTarget());

        pathTarget = new PathTarget(mapper, Article.class, "translations");
        Assert.assertEquals("translations", pathTarget.translatedPath());
    }

    @Test
    public void simpleResolution() {
        getMapper().map(ParentType.class, EmbeddedType.class);
        Mapper mapper = getMapper();
        EntityModel entityModel = mapper.getEntityModel(ParentType.class);

        PathTarget pathTarget = new PathTarget(mapper, entityModel, "name");
        Assert.assertEquals("n", pathTarget.translatedPath());
        Assert.assertEquals(entityModel.getField("name"), pathTarget.getTarget());

        pathTarget = new PathTarget(mapper, entityModel, "n");
        Assert.assertEquals("n", pathTarget.translatedPath());
        Assert.assertEquals(entityModel.getField("n"), pathTarget.getTarget());
    }

    @Test
    public void subClasses() {
        getMapper().map(ParentType.class, EmbeddedType.class, EmbeddedSubtype.class);
        Mapper mapper = getMapper();

        PathTarget pathTarget = new PathTarget(mapper, ParentType.class, "embedded.flag");
        Assert.assertEquals("embedded.flag", pathTarget.translatedPath());
        Assert.assertEquals(mapper.getEntityModel(EmbeddedSubtype.class).getField("flag"), pathTarget.getTarget());
    }

    @Test
    public void disableValidation() {
        getMapper().map(WithNested.class, Nested.class, NestedImpl.class, AnotherNested.class);
        Mapper mapper = getMapper();

        final PathTarget pathTarget = new PathTarget(mapper, WithNested.class, "nested.field.fail", false);
        Assert.assertEquals("nested.field.fail", pathTarget.translatedPath());
        Assert.assertNull(pathTarget.getTarget());
    }

    @Entity
    private static class Grade {
        private int marks;

        @Property("d")
        private Map<String, String> data;

        public Grade() {
        }

        public Grade(int marks, Map<String, String> data) {
            this.marks = marks;
            this.data = data;
        }

        @Override
        public String toString() {
            return ("marks: " + marks + ", data: " + data);
        }
    }

    @Entity
    private static class Student {
        @Id
        private long id;

        private List<Grade> grades;

        public Student() {
        }

        public Student(long id, Grade... grades) {
            this.id = id;
            this.grades = asList(grades);
        }

        @Override
        public String toString() {
            return ("id: " + id + ", grades: " + grades);
        }
    }
}
