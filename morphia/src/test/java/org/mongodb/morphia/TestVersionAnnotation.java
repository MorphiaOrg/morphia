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


import java.util.ConcurrentModificationException;

import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;


/**
 * @author Scott Hernandez
 */

public class TestVersionAnnotation extends TestBase {

  private static class B {
    @Id
    ObjectId id = new ObjectId();
    @Version
    long version;
  }

  @Entity("Test")
  public abstract static class BaseFoo {
    @Id
    private ObjectId id;
    @Version
    private long version;
    private String name;

    // getters/setters ...
  }

  @Entity("Test")
  public static class Foo extends BaseFoo {
    private int value;

    // getters/setters ...
  }

  @Ignore
  @Test(expected = ConcurrentModificationException.class)
  public void testVersion() throws Exception {

    final B b1 = new B();
    try {
      ds.save(b1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    final B b2 = new B();
    b2.id = b1.id;
    ds.save(b2);
  }
  
  @Test
  public void abstractParent() {
    morphia.map(Foo.class);
    morphia.mapPackage(Foo.class.getPackage()
      .toString());
  }
}