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


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.testmodel.Rectangle;
import com.mongodb.BasicDBObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Scott Hernandez
 */
public class TestIdField extends TestBase {

    @Entity
    private static class ReferenceAsId {
        @Id
        @Reference
        Rectangle id;

        protected ReferenceAsId() {
        }

        public ReferenceAsId(final Rectangle key) {
            id = key;
        }
    }

    @Entity
    private static class KeyAsId {
        @Id
        Key<?> id;

        protected KeyAsId() {
        }

        public KeyAsId(final Key<?> key) {
            id = key;
        }
    }

    @Entity
    private static class MapAsId {
        @Id
        final Map<String, String> id = new HashMap<String, String>();
    }

    @Entity(noClassnameStored = true)
    public static class EmbeddedId {

        @Id
        private MyId id;
        private String data;

        public EmbeddedId() {
        }

        public EmbeddedId(final MyId myId, final String data) {
            id = myId;
            this.data = data;
        }
    }

    @Embedded
    public static class MyId {
        private String myIdPart1;
        private String myIdPart2;

        public MyId() {
        }

        public MyId(final String myIdPart1, final String myIdPart2) {
            this.myIdPart1 = myIdPart1;
            this.myIdPart2 = myIdPart2;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final MyId myId = (MyId) o;

            if (!myIdPart1.equals(myId.myIdPart1)) {
                return false;
            }
            if (!myIdPart2.equals(myId.myIdPart2)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = myIdPart1.hashCode();
            result = 31 * result + myIdPart2.hashCode();
            return result;
        }
    }

    @Test
    @Ignore("need to set the _db in the dbRef for this to work... see issue 90")
    public void testReferenceAsId() throws Exception {
        morphia.map(ReferenceAsId.class);

        final Rectangle r = new Rectangle(1, 1);
        final Key<Rectangle> rKey = ds.save(r);

        final ReferenceAsId rai = new ReferenceAsId(r);
        final Key<ReferenceAsId> raiKey = ds.save(rai);
        final ReferenceAsId raiLoaded = ds.get(ReferenceAsId.class, rKey);
        assertNotNull(raiLoaded);
        assertEquals(raiLoaded.id.getArea(), r.getArea(), 0);

        assertNotNull(raiKey);
    }

    @Test
    public void testKeyAsId() throws Exception {
        morphia.map(KeyAsId.class);

        final Rectangle r = new Rectangle(1, 1);
        //        Rectangle r2 = new Rectangle(11,11);

        final Key<Rectangle> rKey = ds.save(r);
        //        Key<Rectangle> r2Key = ds.save(r2);
        final KeyAsId kai = new KeyAsId(rKey);
        final Key<KeyAsId> kaiKey = ds.save(kai);
        final KeyAsId kaiLoaded = ds.get(KeyAsId.class, rKey);
        assertNotNull(kaiLoaded);
        assertNotNull(kaiKey);
    }

    @Test
    public void testMapAsId() throws Exception {
        morphia.map(MapAsId.class);

        final MapAsId mai = new MapAsId();
        mai.id.put("test", "string");
        final Key<MapAsId> maiKey = ds.save(mai);
        final MapAsId maiLoaded = ds.get(MapAsId.class, new BasicDBObject("test", "string"));
        assertNotNull(maiLoaded);
        assertNotNull(maiKey);
    }

    @Test
    public void testIdFieldNameMapping() throws Exception {
        final Rectangle r = new Rectangle(1, 12);
        final BasicDBObject dbObj = (BasicDBObject) morphia.toDBObject(r);
        assertFalse(dbObj.containsField("id"));
        assertTrue(dbObj.containsField(Mapper.ID_KEY));
        assertEquals(4, dbObj.size()); //_id, h, w, className
    }

    @Test
    public void embeddedIds() {
        final MyId id = new MyId("1", "2");

        final EmbeddedId a = new EmbeddedId(id, "data");
        final EmbeddedId b = new EmbeddedId(new MyId("2", "3"), "data, too");

        ds.save(a);
        ds.save(b);

        Assert.assertEquals(a.data, ds.get(EmbeddedId.class, id).data);

        final EmbeddedId embeddedId = ds.find(EmbeddedId.class).field("_id").in(Arrays.asList(id)).asList().get(0);
        Assert.assertEquals(a.data, embeddedId.data);
        Assert.assertEquals(a.id, embeddedId.id);
    }
}