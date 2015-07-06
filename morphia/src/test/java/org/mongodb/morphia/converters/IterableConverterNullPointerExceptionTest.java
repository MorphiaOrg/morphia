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


package org.mongodb.morphia.converters;


import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;


public class IterableConverterNullPointerExceptionTest extends TestBase {
    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(TestEntity.class);
    }

    @Test
    public void testIt() throws Exception {
        final TestEntity te = new TestEntity();
        te.array = new String[]{null, "notNull", null};
        getDs().save(te);

        TestEntity te2 = null;
        try {
            te2 = getDs().find(TestEntity.class).get();
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
        assertArrayEquals(te.array, te2.array);
    }

    @Entity
    static class TestEntity {
        @Id
        private String id;
        private String[] array;
    }
}
