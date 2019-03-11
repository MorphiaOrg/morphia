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


import com.mongodb.BasicDBObject;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.testmodel.Rectangle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Scott Hernandez
 */
public class TestIdField extends TestBase {

    @Test
    public void embeddedIds() {
        final MyId id = new MyId("1", "2");

        final EmbeddedId a = new EmbeddedId(id, "data");
        final EmbeddedId b = new EmbeddedId(new MyId("2", "3"), "data, too");

        getDs().save(a);
        getDs().save(b);

        assertEquals(a.data, getDs().get(EmbeddedId.class, id).data);

        final EmbeddedId embeddedId = getDs().find(EmbeddedId.class).field("_id").in(Arrays.asList(id)).find().next();
        Assert.assertEquals(a.data, embeddedId.data);
        Assert.assertEquals(a.id, embeddedId.id);
    }

    @Test
    public void testIdFieldNameMapping() {
        final Rectangle r = new Rectangle(1, 12);
        final BasicDBObject dbObj = (BasicDBObject) getMorphia().toDBObject(r);
        assertFalse(dbObj.containsField("id"));
        assertTrue(dbObj.containsField("_id"));
        assertEquals(4, dbObj.size()); //_id, h, w, className
    }

    @Test
    public void testKeyAsId() {
        getMorphia().map(KeyAsId.class);

        final Rectangle r = new Rectangle(1, 1);
        //        Rectangle r2 = new Rectangle(11,11);

        final Key<Rectangle> rKey = getDs().save(r);
        //        Key<Rectangle> r2Key = ds.save(r2);
        final KeyAsId kai = new KeyAsId(rKey);
        final Key<KeyAsId> kaiKey = getDs().save(kai);
        final KeyAsId kaiLoaded = getDs().get(KeyAsId.class, rKey);
        assertNotNull(kaiLoaded);
        assertNotNull(kaiKey);
    }

    @Test
    public void testMapAsId() {
        getMorphia().map(MapAsId.class);

        final MapAsId mai = new MapAsId();
        mai.id.put("test", "string");
        final Key<MapAsId> maiKey = getDs().save(mai);
        final MapAsId maiLoaded = getDs().get(MapAsId.class, new BasicDBObject("test", "string"));
        assertNotNull(maiLoaded);
        assertNotNull(maiKey);
    }

    @Entity
    private static class KeyAsId {
        @Id
        private Key<Rectangle> id;

        private KeyAsId() {
        }

        KeyAsId(final Key<Rectangle> key) {
            id = key;
        }
    }

    @Entity
    private static class MapAsId {
        @Id
        private final Map<String, String> id = new HashMap<String, String>();
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
        public int hashCode() {
            int result = myIdPart1.hashCode();
            result = 31 * result + myIdPart2.hashCode();
            return result;
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
    }
}
