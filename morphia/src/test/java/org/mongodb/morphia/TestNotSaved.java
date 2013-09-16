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


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.NotSaved;


/**
 * @author Scott Hernandez
 */
public class TestNotSaved extends TestBase {

  @Entity(value = "Normal", noClassnameStored = true)
  static class Normal {
    @Id ObjectId id = new ObjectId();
    String name;

    public Normal(final String name) {
      this.name = name;
    }

    protected Normal() {
    }
  }

  @Entity(value = "Normal", noClassnameStored = true)
  static class NormalWithNotSaved {
    @Id       ObjectId id   = new ObjectId();
    @NotSaved
    final String   name = "never";
  }

  @Test
  public void testBasic() throws Exception {
    ds.save(new Normal("value"));
    Normal n = ds.find(Normal.class).get();
    Assert.assertNotNull(n);
    Assert.assertNotNull(n.name);
    ds.delete(n);
    ds.save(new NormalWithNotSaved());
    n = ds.find(Normal.class).get();
    Assert.assertNotNull(n);
    Assert.assertNull(n.name);
    ds.delete(n);
    ds.save(new Normal("value21"));
    final NormalWithNotSaved notSaved = ds.find(NormalWithNotSaved.class).get();
    Assert.assertNotNull(notSaved);
    Assert.assertNotNull(notSaved.name);
    Assert.assertEquals("never", notSaved.name);
  }
}