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


import com.mongodb.WriteConcern;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestQuery.ContainsPic;
import org.mongodb.morphia.TestQuery.Pic;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.query.ValidationException;
import org.mongodb.morphia.testmodel.Circle;
import org.mongodb.morphia.testmodel.Rectangle;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * @author Scott Hernandez
 */
public class TestUpdateOps extends TestBase {

    private static class ContainsIntArray {
        @Id
        private ObjectId id;
        private final Integer[] values = {1, 2, 3};
    }

    private static class ContainsInt {
        @Id
        private ObjectId id;
        private int val;
    }

    @Entity
    private static class ContainsPicKey {
        @Id
        private ObjectId id;
        private String name = "test";
        private Key<Pic> pic;
    }

    @Test
    public void testIncDec() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(10, 10),
                                   new Rectangle(10, 10)};

        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        final Query<Rectangle> q1 = getDs().find(Rectangle.class, "height", 1D);
        final Query<Rectangle> q2 = getDs().find(Rectangle.class, "height", 2D);

        assertEquals(3, getDs().getCount(q1));
        assertEquals(0, getDs().getCount(q2));

        final UpdateResults<Rectangle> results = getDs().update(q1, getDs().createUpdateOperations(Rectangle.class).inc("height"));
        assertUpdated(results, 3);

        assertEquals(0, getDs().getCount(q1));
        assertEquals(3, getDs().getCount(q2));

        getDs().update(q2, getDs().createUpdateOperations(Rectangle.class).dec("height"));
        assertEquals(3, getDs().getCount(q1));
        assertEquals(0, getDs().getCount(q2));

        getDs().update(getDs().find(Rectangle.class, "width", 1D),
                       getDs().createUpdateOperations(Rectangle.class).set("height", 1D).set("width", 1D),
                       true);
        assertNotNull(getDs().find(Rectangle.class, "width", 1D).get());
        assertNull(getDs().find(Rectangle.class, "width", 2D).get());
        getDs().update(getDs().find(Rectangle.class, "width", 1D),
                       getDs().createUpdateOperations(Rectangle.class).set("height", 2D).set("width", 2D),
                       true);
        assertNull(getDs().find(Rectangle.class, "width", 1D).get());
        assertNotNull(getDs().find(Rectangle.class, "width", 2D).get());
    }

    @Test
    public void testInsertUpdate() throws Exception {
        final UpdateResults<Circle> res = getDs().update(getDs().createQuery(Circle.class).field("radius").equal(0),
                                                         getDs().createUpdateOperations(Circle.class).inc("radius", 1D), true);
        assertInserted(res);
    }

    @Test
    public void testSetUnset() throws Exception {
        final Key<Circle> key = getDs().save(new Circle(1));

        UpdateResults<Circle> res = getDs().updateFirst(getDs().find(Circle.class, "radius", 1D),
                                                        getDs().createUpdateOperations(Circle.class).set("radius", 2D));

        assertUpdated(res, 1);

        final Circle c = getDs().getByKey(Circle.class, key);
        assertEquals(2D, c.getRadius(), 0);


        res = getDs().updateFirst(getDs().find(Circle.class, "radius", 2D), getDs().createUpdateOperations(Circle.class).unset("radius"));
        assertUpdated(res, 1);

        final Circle c2 = getDs().getByKey(Circle.class, key);
        assertEquals(0D, c2.getRadius(), 0);
    }
    
    @Test
    public void testSetOnInsertWhenInserting() throws Exception {
        checkMinServerVersion(2.4);
        ObjectId id = new ObjectId();
        UpdateResults<Circle> res = getDs().updateFirst(
                getDs().createQuery(Circle.class).field("id").equal(id), 
                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 2D), true);
        
        assertInserted(res);
        
        final Circle c = getDs().get(Circle.class, id);
        
        assertNotNull(c);
        assertEquals(2D, c.getRadius(), 0);
    }
    
    @Test
    public void testSetOnInsertWhenUpdating() throws Exception {
        checkMinServerVersion(2.4);
        ObjectId id = new ObjectId();
        UpdateResults<Circle> res = getDs().updateFirst(
                getDs().createQuery(Circle.class).field("id").equal(id), 
                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 1D), true);
        
        assertInserted(res);
        
        res = getDs().updateFirst(
                getDs().createQuery(Circle.class).field("id").equal(id), 
                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 2D), true);
        
        assertUpdated(res, 1);
        
        final Circle c = getDs().get(Circle.class, id);
        
        assertNotNull(c);
        assertEquals(1D, c.getRadius(), 0);
    }


    @Test(expected = ValidationException.class)
    public void testValidationBadFieldName() throws Exception {
        getDs().update(getDs().createQuery(Circle.class).field("radius").equal(0),
                       getDs().createUpdateOperations(Circle.class).inc("r", 1D),
                       true,
                       WriteConcern.SAFE);
        Assert.assertTrue(false); //should not get here.
    }

    @Test
    @Ignore("need to inspect the logs")
    public void testValidationBadFieldType() throws Exception {
        try {
            getDs().update(getDs().createQuery(Circle.class).field("radius").equal(0),
                           getDs().createUpdateOperations(Circle.class).set("radius", "1"),
                           true,
                           WriteConcern.SAFE);
            Assert.assertTrue(false); //should not get here.
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains("inconsistent"));
        }
    }

    @Test
    public void testInsertUpdatesUnsafe() throws Exception {
        getDs().getDB()
            .requestStart();
        try {
            getDs().update(getDs().createQuery(Circle.class)
                               .field("radius")
                               .equal(0), getDs().createUpdateOperations(Circle.class)
                                              .inc("radius", 1D), true, WriteConcern.NONE);
            assertEquals(1, getDs().getCount(Circle.class));
        } finally {
            getDs().getDB()
                .requestDone();
        }
    }

    @Test
    public void testUpdateWithDifferentType() throws Exception {
        final ContainsInt cInt = new ContainsInt();
        cInt.val = 21;
        getDs().save(cInt);

        final UpdateResults<ContainsInt> res = getDs().updateFirst(getDs().createQuery(ContainsInt.class),
                                                                   getDs().createUpdateOperations(ContainsInt.class)
                                                                       .inc("val", 1.1D));
        assertUpdated(res, 1);

        final ContainsInt ciLoaded = getDs().find(ContainsInt.class).limit(1).get();
        assertEquals(22, ciLoaded.val);
    }

    @Test
    public void testRemoveFirst() throws Exception {
        final ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        ContainsIntArray cIALoaded = getDs().get(cIntArray);
        assertEquals(3, cIALoaded.values.length);
        assertArrayEquals((new ContainsIntArray()).values, cIALoaded.values);

        //remove 1
        UpdateResults<ContainsIntArray> res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                                                  getDs().createUpdateOperations(ContainsIntArray.class)
                                                                      .removeFirst("values"));
        assertUpdated(res, 1);
        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{2, 3}, cIALoaded.values);

        //remove 3
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).removeLast("values"));
        assertUpdated(res, 1);
        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{2}, cIALoaded.values);
    }

    private void assertUpdated(final UpdateResults res, final int count) {
        assertEquals(0, res.getInsertedCount());
        assertEquals(count, res.getUpdatedCount());
        assertEquals(true, res.getUpdatedExisting());
    }

    private void assertInserted(final UpdateResults res) {
        assertEquals(1, res.getInsertedCount());
        assertEquals(0, res.getUpdatedCount());
        assertEquals(false, res.getUpdatedExisting());
    }

    @Test
    public void testAdd() throws Exception {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        ContainsIntArray cIALoaded = getDs().get(cIntArray);
        assertEquals(3, cIALoaded.values.length);
        assertArrayEquals((new ContainsIntArray()).values, cIALoaded.values);

        //add 4 to array
        UpdateResults<ContainsIntArray> res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                                                  getDs().createUpdateOperations(ContainsIntArray.class)
                                                                      .add("values", 4, false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, cIALoaded.values);

        //add unique (4) -- noop
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).add("values", 4, false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, cIALoaded.values);

        //add dup 4
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).add("values", 4, true));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 4}, cIALoaded.values);

        //cleanup for next tests
        getDs().delete(getDs().find(ContainsIntArray.class));
        cIntArray = getDs().getByKey(ContainsIntArray.class, getDs().save(new ContainsIntArray()));

        //add [4,5]
        final List<Integer> newValues = new ArrayList<Integer>();
        newValues.add(4);
        newValues.add(5);
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).addAll("values",
                                                                                                newValues,
                                                                                                false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, cIALoaded.values);

        //add them again... noop
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).addAll("values",
                                                                                                newValues,
                                                                                                false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, cIALoaded.values);

        //add dups [4,5]
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).addAll("values",
                                                                                                newValues,
                                                                                                true));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5, 4, 5}, cIALoaded.values);

    }

    @Test
    public void testExistingUpdates() throws Exception {
        Circle c = new Circle(100D);
        getDs().save(c);
        c = new Circle(12D);
        getDs().save(c);
        UpdateResults<Circle> res = getDs().updateFirst(getDs().createQuery(Circle.class),
                                                        getDs().createUpdateOperations(Circle.class).inc("radius", 1D));
        assertUpdated(res, 1);

        res = getDs().update(getDs().createQuery(Circle.class), getDs().createUpdateOperations(Circle.class).inc("radius"));
        assertUpdated(res, 2);

        //test possible data type change.
        final Circle cLoaded = getDs().find(Circle.class, "radius", 13).get();
        assertNotNull(cLoaded);
        assertEquals(13D, cLoaded.getRadius(), 0D);
    }

    @Test
    @Ignore("waiting on SERVER-1470 bug; dbRef is not included from query on upsert")
    public void testInsertWithRef() throws Exception {
        final Pic pic = new Pic();
        pic.setName("fist");
        final Key<Pic> picKey = getDs().save(pic);

        //test with Key<Pic>
        UpdateResults<ContainsPic> res = getDs().updateFirst(getDs().find(ContainsPic.class, "name", "first").filter("pic", picKey),
                                                             getDs().createUpdateOperations(ContainsPic.class).set("name", "A"), true);

        assertInserted(res);
        assertEquals(1, getDs().find(ContainsPic.class).countAll());

        getDs().delete(getDs().find(ContainsPic.class));

        //test with pic object
        res = getDs().updateFirst(getDs().find(ContainsPic.class, "name", "first").filter("pic", pic),
                                  getDs().createUpdateOperations(ContainsPic.class).set(
                                                                                           "name", "second"),
                                  true);

        assertInserted(res);
        assertEquals(1, getDs().find(ContainsPic.class).countAll());

        //test reading the object.
        final ContainsPic cp = getDs().find(ContainsPic.class).get();
        assertNotNull(cp);
        assertEquals("second", cp.getName());
        assertNotNull(cp.getPic());
        assertNotNull(cp.getPic().getName());
        assertEquals("fist", cp.getPic().getName());

    }

    @Test
    public void testUpdateRef() throws Exception {
        final ContainsPic cp = new ContainsPic();
        cp.setName("cp one");

        getDs().save(cp);

        final Pic pic = new Pic();
        pic.setName("fist");
        final Key<Pic> picKey = getDs().save(pic);


        //test with Key<Pic>
        final UpdateResults<ContainsPic> res = getDs().updateFirst(getDs().find(ContainsPic.class, "name", cp.getName()),
                                                                   getDs().createUpdateOperations(ContainsPic.class).set("pic", pic));

        assertEquals(1, res.getUpdatedCount());

        //test reading the object.
        final ContainsPic cp2 = getDs().find(ContainsPic.class).get();
        assertNotNull(cp2);
        assertEquals(cp2.getName(), cp.getName());
        assertNotNull(cp2.getPic());
        assertNotNull(cp2.getPic().getName());
        assertEquals(cp2.getPic().getName(), pic.getName());

        getDs().updateFirst(getDs().find(ContainsPic.class, "name", cp.getName()),
                            getDs().createUpdateOperations(ContainsPic.class).set("pic", picKey));

        //test reading the object.
        final ContainsPic cp3 = getDs().find(ContainsPic.class).get();
        assertNotNull(cp3);
        assertEquals(cp3.getName(), cp.getName());
        assertNotNull(cp3.getPic());
        assertNotNull(cp3.getPic().getName());
        assertEquals(cp3.getPic().getName(), pic.getName());
    }

    @Test
    public void testUpdateKeyRef() throws Exception {
        final ContainsPicKey cpk = new ContainsPicKey();
        cpk.name = "cpk one";

        getDs().save(cpk);

        final Pic pic = new Pic();
        pic.setName("fist again");
        final Key<Pic> picKey = getDs().save(pic);
        // picKey = getDs().getKey(pic);


        //test with Key<Pic>
        final UpdateResults<ContainsPicKey> res = getDs().updateFirst(getDs().find(ContainsPicKey.class, "name", cpk.name),
                                                                      getDs().createUpdateOperations(ContainsPicKey.class).set("pic", pic));

        assertEquals(1, res.getUpdatedCount());

        //test reading the object.
        final ContainsPicKey cpk2 = getDs().find(ContainsPicKey.class).get();
        assertNotNull(cpk2);
        assertEquals(cpk2.name, cpk.name);
        assertNotNull(cpk2.pic);
        assertEquals(cpk2.pic, picKey);

        getDs().updateFirst(getDs().find(ContainsPicKey.class, "name", cpk.name),
                            getDs().createUpdateOperations(ContainsPicKey.class).set("pic", picKey));

        //test reading the object.
        final ContainsPicKey cpk3 = getDs().find(ContainsPicKey.class).get();
        assertNotNull(cpk3);
        assertEquals(cpk3.name, cpk.name);
        assertNotNull(cpk3.pic);
        assertEquals(cpk3.pic, picKey);

    }

}
