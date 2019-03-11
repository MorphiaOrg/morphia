/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia.indexes;

import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.MappedClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Scott Hernandez
 */
public class TestIndexInheritance extends TestBase {

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

    @Indexes(@Index(fields = @Field("description")))
    public abstract static class Shape {
        @Id
        private ObjectId id;
        private String description;
        @Indexed
        private String foo;

        public String getDescription() {
            return description;
        }

        void setDescription(final String description) {
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

    @Indexes(@Index(fields = @Field("radius")))
    private static class Circle extends Shape {
        private double radius = 1;

        Circle() {
            setDescription("Circles are round and can be rolled along the ground.");
        }
    }

}
