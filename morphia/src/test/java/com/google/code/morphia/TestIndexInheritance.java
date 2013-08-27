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


package com.google.code.morphia;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.mapping.MappedClass;
import com.mongodb.DBCollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Scott Hernandez
 */
public class TestIndexInheritance extends TestBase {

  @Indexes(@Index("description"))
  private abstract static class Shape {
    @Id
    ObjectId id;
    String description;
    @Indexed
    String foo;
  }

  @Indexes(@Index("radius"))
  private static class Circle extends Shape {
    double radius = 1;

    public Circle() {
      description = "Circles are round and can be rolled along the ground.";
    }
  }

  @Entity
  private static class Child extends Father {
    private Child() {
    }

    private Child(final String name) {
      super(name);
    }
  }

  @Entity
  private static class Father extends GrandFather {
    private Father() {
    }

    private Father(final String name) {
      super(name);
    }
  }

  @Entity
  private static class GrandFather {
    @Id
    ObjectId id;
    String name;

    private GrandFather() {
    }

    private GrandFather(final String name) {
      this.name = name;
    }
  }

  @Test
  public void testClassIndexInherit() throws Exception {
    morphia.map(Circle.class)
      .map(Shape.class);
    final MappedClass mc = morphia.getMapper()
      .getMappedClass(Circle.class);
    assertNotNull(mc);

    assertEquals(2, mc.getAnnotations(Indexes.class)
      .size());

    ds.ensureIndexes();
    final DBCollection coll = ds.getCollection(Circle.class);

    assertEquals(4, coll.getIndexInfo()
      .size());
  }

  @Test
  public void testInheritedFieldIndex() throws Exception {
    morphia.map(Circle.class)
      .map(Shape.class);
    morphia.getMapper()
      .getMappedClass(Circle.class);

    ds.ensureIndexes();
    final DBCollection coll = ds.getCollection(Circle.class);

    assertEquals(4, coll.getIndexInfo()
      .size());
  }

  @Test
  public void deepTree() {
    final Child jimmy = new Child("Jimmy");
    ds.save(jimmy);

    final Child loaded = ds.get(Child.class, jimmy.id);
    Assert.assertNotNull(loaded);
    Assert.assertEquals(jimmy.name, loaded.name);
  }
}
