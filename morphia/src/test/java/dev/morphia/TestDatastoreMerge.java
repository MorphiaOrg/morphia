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


package dev.morphia;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import org.bson.types.ObjectId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestDatastoreMerge extends TestBase {

    @Test
    public void testMerge() {
        final Merger te = new Merger();
        te.name = "test1";
        te.foo = "bar";
        te.position = 1;
        getDs().save(te);

        assertEquals(1, getDs().getCount(te));

        //only update the position field with merge, normally save would override the whole object.
        final Merger te2 = new Merger();
        te2.id = te.id;
        te2.position = 5;
        getDs().merge(te2);

        final Merger teLoaded = getDs().get(te);
        assertEquals(te.name, teLoaded.name);
        assertEquals(te.foo, teLoaded.foo);
        assertEquals(te2.position, teLoaded.position);
    }

    @Test
    public void merge() {
        Datastore ds = getDs();

        Test1 test1 = new Test1();
        test1.name = "foobar";
        ds.save(test1);

        Test2 test2 = ds.createQuery(Test2.class).get();
        assertNotNull(test2.id);
        test2.blarg = "barfoo";
        ds.merge(test2);

        test1 = ds.createQuery(Test1.class).field("_id").equal(test1.id).get();

        assertNotNull(test1.name);//fails
    }

    private static class Merger {
        @Id
        private ObjectId id;
        private String name;
        private String foo;
        private int position;
    }

    @Entity(value = "test", noClassnameStored = true)
    static class Test1 {
        @Id
        ObjectId id;
        String name;
        @Version
        long version;
    }

    @Entity(value = "test", noClassnameStored = true)
    static class Test2 {
        @Id
        ObjectId id;
        String blarg;
        @Version
        long version;
    }
}
