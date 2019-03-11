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


import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.annotations.Id;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Scott Hernandez
 */
public class TestDatatypes extends TestBase {

    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(ContainsByte.class, ContainsDouble.class, ContainsFloat.class, ContainsShort.class);
    }

    @Test
    public void testByte() throws Exception {
        final ContainsByte cb = new ContainsByte();
        getDs().save(cb);
        final ContainsByte loaded = getDs().get(cb);

        assertNotNull(loaded);
        assertTrue(loaded.val0 == cb.val0);
        assertTrue(loaded.val1.equals(cb.val1));
    }

    @Test
    public void testFloat() throws Exception {
        final ContainsFloat cf = new ContainsFloat();
        getDs().save(cf);
        final ContainsFloat loaded = getDs().get(cf);

        assertNotNull(loaded);
        assertTrue(loaded.val0 == cf.val0);
        assertTrue(loaded.val1.equals(cf.val1));
    }

    @Test
    public void testShort() throws Exception {
        final ContainsShort cs = new ContainsShort();
        getDs().save(cs);
        final ContainsShort loaded = getDs().get(cs);

        assertNotNull(loaded);
        assertTrue(loaded.val0 == cs.val0);
        assertTrue(loaded.val1.equals(cs.val1));
    }

    public static class ContainsFloat {
        private final float val0 = 1.1F;
        private final Float val1 = 1.1F;
        @Id
        private ObjectId id;
    }

    public static class ContainsDouble {
        @Id
        private ObjectId id;
        private double val0 = 1.1D;
        private Double val1 = 1.1D;
    }

    public static class ContainsShort {
        private final short val0 = 1;
        private final Short val1 = 1;
        @Id
        private ObjectId id;
    }

    public static class ContainsByte {
        private final byte val0 = 1;
        private final Byte val1 = 1;
        @Id
        private ObjectId id;
    }
}
