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
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TestDatatypes extends TestBase {

    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMapper().map(ContainsByte.class, ContainsDouble.class, ContainsFloat.class, ContainsShort.class);
    }

    @Test
    public void testByte() {
        final ContainsByte cb = new ContainsByte();
        getDs().save(cb);
        final ContainsByte loaded = getDs().find(ContainsByte.class)
                                           .filter(eq("_id", cb.id))
                                           .first();

        assertNotNull(loaded);
        assertTrue(loaded.val0 == cb.val0);
        assertTrue(loaded.val1.equals(cb.val1));
    }

    @Test
    public void testFloat() {
        final ContainsFloat cf = new ContainsFloat();
        getDs().save(cf);
        final ContainsFloat loaded = getDs().find(ContainsFloat.class)
                                            .filter(eq("_id", cf.id))
                                            .first();

        assertNotNull(loaded);
        assertTrue(loaded.val0 == cf.val0);
        assertTrue(loaded.val1.equals(cf.val1));
    }

    @Test
    public void testShort() {
        final ContainsShort cs = new ContainsShort();
        getDs().save(cs);
        final ContainsShort loaded = getDs().find(ContainsShort.class)
                                            .filter(eq("_id", cs.id))
                                            .first();

        assertNotNull(loaded);
        assertTrue(loaded.val0 == cs.val0);
        assertTrue(loaded.val1.equals(cs.val1));
    }

    @Entity
    public static class ContainsFloat {
        @Id
        private ObjectId id;
        private final float val0 = 1.1F;
        private final Float val1 = 1.1F;
    }

    @Entity
    public static class ContainsDouble {
        @Id
        private ObjectId id;
        private final double val0 = 1.1D;
        private final Double val1 = 1.1D;
    }

    @Entity
    public static class ContainsShort {
        @Id
        private ObjectId id;
        private final short val0 = 1;
        private final Short val1 = 1;
    }

    @Entity
    public static class ContainsByte {
        @Id
        private ObjectId id;
        private final byte val0 = 1;
        private final Byte val1 = 1;
    }
}
