/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */


package org.mongodb.morphia;


import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.annotations.Id;

import static org.junit.Assert.assertEquals;


/**
 * @author Scott Hernandez
 */
public class TestDatastoreMerge extends TestBase {

    @Test
    public void testMerge() throws Exception {
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

    private static class Merger {
        @Id
        private ObjectId id;
        private String name;
        private String foo;
        private int position;
    }
}
