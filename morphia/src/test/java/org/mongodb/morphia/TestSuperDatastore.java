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


import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.testmodel.Rectangle;

import static org.junit.Assert.assertEquals;


/**
 * @author Scott Hernandez
 */
public class TestSuperDatastore extends TestBase {

    @Before
    @Override
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testSaveAndDelete() throws Exception {
        final String ns = "hotels";
        final Rectangle rect = new Rectangle(10, 10);
        ObjectId id = new ObjectId();
        rect.setId(id);

        getDb().getCollection(ns).remove(new BasicDBObject());

        //test delete(entity, id)
        getAds().save(ns, rect);
        assertEquals(1, getAds().getCount(ns));
        getAds().delete(ns, Rectangle.class, 1);
        assertEquals(1, getAds().getCount(ns));
        getAds().delete(ns, Rectangle.class, id);
        assertEquals(0, getAds().getCount(ns));
    }

    @Test
    public void testGet() throws Exception {
        final String ns = "hotels";
        final Rectangle rect = new Rectangle(10, 10);

        getDb().getCollection(ns).remove(new BasicDBObject());

        //test delete(entity, id)
        getAds().save(ns, rect);
        assertEquals(1, getAds().getCount(ns));
        final Rectangle rectLoaded = getAds().get(ns, Rectangle.class, rect.getId());
        assertEquals(rect.getId(), rectLoaded.getId());
        assertEquals(rect.getArea(), rectLoaded.getArea(), 0);
    }

    @Test
    public void testFind() throws Exception {
        final String ns = "hotels";
        Rectangle rect = new Rectangle(10, 10);
        ObjectId id = new ObjectId();
        rect.setId(id);

        getDb().getCollection(ns).remove(new BasicDBObject());

        //test delete(entity, id)
        getAds().save(ns, rect);
        assertEquals(1, getAds().getCount(ns));
        Rectangle rectLoaded = getAds().find(ns, Rectangle.class).get();
        assertEquals(rect.getId(), rectLoaded.getId());
        assertEquals(rect.getArea(), rectLoaded.getArea(), 0);

        rect = new Rectangle(2, 1);
        getAds().save(rect); //saved to default collection name (kind)
        assertEquals(1, getAds().getCount(rect));

        rect.setId(null);
        getAds().save(rect); //saved to default collection name (kind)
        assertEquals(2, getAds().getCount(rect));

        rect = new Rectangle(4, 3);
        getAds().save(ns, rect);
        assertEquals(2, getAds().getCount(ns));

        rectLoaded = getAds().find(ns, Rectangle.class).asList().get(1);
        assertEquals(rect.getId(), rectLoaded.getId());
        assertEquals(rect.getArea(), rectLoaded.getArea(), 0);

        getAds().find(ns, Rectangle.class, "_id !=", "-1", 1, 1).get();

    }
}
