/*
  Copyright (C) 2010 Scott Hernandez
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


package dev.morphia.mapping;


import dev.morphia.annotations.Entity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.ConstructorArgs;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;


@Ignore("come back to this one")
public class TestConstructorArgs extends TestBase {

    @Test
    public void testBasic() {
        Normal n = new Normal();
        final ObjectId acId = n.ac.id;

        getDs().save(n);
        n = getDs().find(Normal.class).execute(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(n);
        Assert.assertNotNull(n.ac);
        Assert.assertEquals(acId, n.ac.id);
    }

    @Entity
    private static class Normal {
        @Id
        private ObjectId id = new ObjectId();
        @ConstructorArgs("_id")
        private final ArgsConstructor ac = new ArgsConstructor(new ObjectId());
    }

    @Entity
    private static final class ArgsConstructor {
        @Id
        private final ObjectId id;

        private ArgsConstructor(final ObjectId id) {
            this.id = id;
        }
    }
}
