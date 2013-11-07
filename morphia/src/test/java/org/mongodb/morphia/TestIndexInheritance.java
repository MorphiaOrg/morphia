/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.mongodb.morphia;


import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Scott Hernandez
 */
public class TestIndexInheritance extends TestBase {

    @Indexes(@Index("description"))
    public abstract static class Shape {
        @Id
        private ObjectId id;
        private String description;
        @Indexed
        private String foo;

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }
    }

    @Indexes(@Index("radius"))
    private static class Circle extends Shape {
        private double radius = 1;

        public Circle() {
            setDescription("Circles are round and can be rolled along the ground.");
        }
    }

    @Entity
    public static class Child extends Father {
        public Child() {
        }

        public Child(final String name) {
            super(name);
        }
    }

    @Entity
    public static class Father extends GrandFather {
        public Father() {
        }

        public Father(final String name) {
            super(name);
        }
    }

    @Entity
    public static class GrandFather {
        @Id
        private ObjectId id;
        private String name;

        public GrandFather() {
        }

        public GrandFather(final String name) {
            this.name = name;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void testClassIndexInherit() throws Exception {
        getMorphia().map(Circle.class)
            .map(Shape.class);
        final MappedClass mc = getMorphia().getMapper()
                                   .getMappedClass(Circle.class);
        assertNotNull(mc);

        assertEquals(2, mc.getAnnotations(Indexes.class)
                          .size());

        getDs().ensureIndexes();
        final DBCollection coll = getDs().getCollection(Circle.class);

        assertEquals(4, coll.getIndexInfo()
                            .size());
    }

    @Test
    public void testInheritedFieldIndex() throws Exception {
        getMorphia().map(Circle.class)
            .map(Shape.class);
        getMorphia().getMapper()
            .getMappedClass(Circle.class);

        getDs().ensureIndexes();
        final DBCollection coll = getDs().getCollection(Circle.class);

        assertEquals(4, coll.getIndexInfo()
                            .size());
    }

    @Test
    public void deepTree() {
        final Child jimmy = new Child("Jimmy");
        getDs().save(jimmy);

        final Child loaded = getDs().get(Child.class, jimmy.getId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(jimmy.getName(), loaded.getName());
    }
}
