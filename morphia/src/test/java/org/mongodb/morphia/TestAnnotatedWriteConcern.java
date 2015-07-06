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


import com.mongodb.WriteConcern;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Scott Hernandez
 */
public class TestAnnotatedWriteConcern extends TestBase {

    @Test
    public void defaultWriteConcern() throws Exception {
        boolean failed = false;
        try {
            getAds().insert(new Simple("simple"), getDs().getDefaultWriteConcern());
            getAds().insert(new Simple("simple"), getDs().getDefaultWriteConcern());
        } catch (Exception e) {
            failed = true;
        }
        assertEquals(1L, getDs().getCount(Simple.class));
        assertTrue("Duplicate Exception was raised!", failed);
    }

    @Test
    public void noneWriteConcern() throws Exception {
        getDs().setDefaultWriteConcern(WriteConcern.UNACKNOWLEDGED);
        try {
            getAds().insert(new Simple("simple"));
            getAds().insert(new Simple("simple"));
            fail("Duplicate Exception was not raised!");
        } catch (Exception e) {
            assertEquals(1L, getDs().getCount(Simple.class));
        }
    }

    @Test
    public void safeWriteConcern() throws Exception {
        boolean failed = false;
        try {
            getAds().insert(new Simple("simple"));
            getAds().insert(new Simple("simple"), WriteConcern.SAFE);
        } catch (Exception e) {
            failed = true;
        }
        assertEquals(1L, getDs().getCount(Simple.class));
        assertTrue("Duplicate Exception was raised!", failed);
    }

    @Entity(concern = "Safe")
    static class Simple {
        @Id
        private String id;

        public Simple(final String id) {
            this();
            this.id = id;
        }

        private Simple() {
        }
    }
}
