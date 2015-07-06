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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestQuery.ContainsPic;
import org.mongodb.morphia.TestQuery.Pic;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.query.ValidationException;
import org.mongodb.morphia.testmodel.Circle;
import org.mongodb.morphia.testmodel.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Scott Hernandez
 */
@SuppressWarnings("UnusedDeclaration") // Morphia uses the fields
public class TestUpdateOps extends TestBase {

    @Test
    public void shouldUpdateAnArrayElement() {
        // given
        ObjectId parentId = new ObjectId();
        String childName = "Bob";
        String updatedLastName = "updatedLastName";

        Parent parent = new Parent();
        parent.id = parentId;
        parent.children.add(new Child("Anthony", "Child"));
        parent.children.add(new Child(childName, "originalLastName"));
        getDs().save(parent);

        // when
        Query<Parent> query = getDs().createQuery(Parent.class)
                                     .field("_id").equal(parentId)
                                     .field("children.first").equal(childName);
        UpdateOperations<Parent> updateOps = getDs().createUpdateOperations(Parent.class)
                                                    .set("children.$.last", updatedLastName);
        UpdateResults updateResults = getDs().update(query, updateOps);

        // then
        assertThat(updateResults.getUpdatedCount(), is(1));
        assertThat(getDs().find(Parent.class, "id", parentId).get().children, hasItem(new Child(childName, updatedLastName)));
    }

    @Test
    public void testAdd() throws Exception {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        ContainsIntArray cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values.length, is(3));
        assertThat(cIALoaded.values, is((new ContainsIntArray()).values));

        //add 4 to array
        UpdateResults res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                                getDs().createUpdateOperations(ContainsIntArray.class).add("values", 4, false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{1, 2, 3, 4}));

        //add unique (4) -- noop
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).add("values", 4, false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{1, 2, 3, 4}));

        //add dup 4
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).add("values", 4, true));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{1, 2, 3, 4, 4}));

        //cleanup for next tests
        getDs().delete(getDs().find(ContainsIntArray.class));
        cIntArray = getDs().getByKey(ContainsIntArray.class, getDs().save(new ContainsIntArray()));

        //add [4,5]
        final List<Integer> newValues = new ArrayList<Integer>();
        newValues.add(4);
        newValues.add(5);
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).addAll("values", newValues, false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{1, 2, 3, 4, 5}));

        //add them again... noop
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).addAll("values", newValues, false));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{1, 2, 3, 4, 5}));

        //add dups [4,5]
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).addAll("values", newValues, true));
        assertUpdated(res, 1);

        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{1, 2, 3, 4, 5, 4, 5}));

    }

    @Test
    public void testExistingUpdates() throws Exception {
        Circle c = new Circle(100D);
        getDs().save(c);
        c = new Circle(12D);
        getDs().save(c);
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class),
                                                getDs().createUpdateOperations(Circle.class).inc("radius", 1D));
        assertUpdated(res, 1);

        res = getDs().update(getDs().createQuery(Circle.class), getDs().createUpdateOperations(Circle.class).inc("radius"));
        assertUpdated(res, 2);

        //test possible data type change.
        final Circle updatedCircle = getDs().find(Circle.class, "radius", 13).get();
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(13D));
    }

    @Test
    public void testIncDec() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(10, 10),
                                   new Rectangle(10, 10)};

        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        final Query<Rectangle> heightOf1 = getDs().find(Rectangle.class, "height", 1D);
        final Query<Rectangle> heightOf2 = getDs().find(Rectangle.class, "height", 2D);

        assertThat(getDs().getCount(heightOf1), is(3L));
        assertThat(getDs().getCount(heightOf2), is(0L));

        final UpdateResults results = getDs().update(heightOf1, getDs().createUpdateOperations(Rectangle.class)
                                                                       .inc("height"));
        assertUpdated(results, 3);

        assertThat(getDs().getCount(heightOf1), is(0L));
        assertThat(getDs().getCount(heightOf2), is(3L));

        getDs().update(heightOf2, getDs().createUpdateOperations(Rectangle.class).dec("height"));
        assertThat(getDs().getCount(heightOf1), is(3L));
        assertThat(getDs().getCount(heightOf2), is(0L));

        getDs().update(getDs().find(Rectangle.class, "height", 1D),
                       getDs().createUpdateOperations(Rectangle.class)
                              .set("height", 1D)
                              .inc("width", 20D));

        assertThat(getDs().getCount(Rectangle.class), is(5L));
        assertThat(getDs().find(Rectangle.class, "height", 1D).get(), is(notNullValue()));
        assertThat(getDs().find(Rectangle.class, "width", 30D).get(), is(notNullValue()));

        getDs().update(getDs().find(Rectangle.class, "width", 30D),
                       getDs().createUpdateOperations(Rectangle.class).set("height", 2D).set("width", 2D));
        assertThat(getDs().find(Rectangle.class, "width", 1D).get(), is(nullValue()));
        assertThat(getDs().find(Rectangle.class, "width", 2D).get(), is(notNullValue()));
    }

    @Test
    public void testInsertUpdate() throws Exception {
        final UpdateResults res = getDs().update(getDs().createQuery(Circle.class).field("radius").equal(0),
                                                 getDs().createUpdateOperations(Circle.class).inc("radius", 1D), true);
        assertInserted(res);
    }

    @Test
    public void testInsertUpdatesUnsafe() throws Exception {
        getDs().update(getDs().createQuery(Circle.class).field("radius").equal(0),
                       getDs().createUpdateOperations(Circle.class).inc("radius", 1D), true, WriteConcern.UNACKNOWLEDGED);
        assertThat(getDs().getCount(Circle.class), is(1L));
    }

    @Test
    public void testInsertWithRef() throws Exception {
        final Pic pic = new Pic();
        pic.setName("fist");
        final Key<Pic> picKey = getDs().save(pic);

        //test with Key<Pic>
        UpdateResults res = getDs().updateFirst(getDs().find(ContainsPic.class, "name", "first").filter("pic", picKey),
                                                getDs().createUpdateOperations(ContainsPic.class).set("name", "A"), true);

        assertInserted(res);
        assertThat(getDs().find(ContainsPic.class).countAll(), is(1L));

        getDs().delete(getDs().find(ContainsPic.class));

        //test with pic object
        res = getDs().updateFirst(getDs().find(ContainsPic.class, "name", "first").filter("pic", pic),
                                  getDs().createUpdateOperations(ContainsPic.class).set("name", "second"), true);

        assertInserted(res);
        assertThat(getDs().find(ContainsPic.class).countAll(), is(1L));

        //test reading the object.
        final ContainsPic cp = getDs().find(ContainsPic.class).get();
        assertThat(cp, is(notNullValue()));
        assertThat(cp.getName(), is("second"));
        assertThat(cp.getPic(), is(notNullValue()));
        assertThat(cp.getPic().getName(), is(notNullValue()));
        assertThat(cp.getPic().getName(), is("fist"));

    }

    @Test
    public void testMaxKeepsCurrentDocumentValueWhenThisIsLargerThanSuppliedValue() throws Exception {
        checkMinServerVersion(2.6);
        // given
        final ObjectId id = new ObjectId();
        final double originalValue = 2D;
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", originalValue), true);

        assertInserted(res);

        // when
        res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                  getDs().createUpdateOperations(Circle.class).max("radius", 1D), true);

        // then
        assertUpdated(res, 1);

        final Circle updatedCircle = getDs().get(Circle.class, id);
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(originalValue));
    }

    @Test
    public void testMaxUsesSuppliedValueWhenThisIsLargerThanCurrentDocumentValue() throws Exception {
        checkMinServerVersion(2.6);
        // given
        final ObjectId id = new ObjectId();
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 1D), true);

        assertInserted(res);

        // when
        final double newHigherValue = 2D;
        res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                  getDs().createUpdateOperations(Circle.class).max("radius", newHigherValue), true);

        // then
        assertUpdated(res, 1);

        final Circle updatedCircle = getDs().get(Circle.class, id);
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(newHigherValue));
    }

    @Test
    public void testMinKeepsCurrentDocumentValueWhenThisIsSmallerThanSuppliedValue() throws Exception {
        checkMinServerVersion(2.6);
        // given
        final ObjectId id = new ObjectId();
        final double originalValue = 3D;
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", originalValue), true);

        assertInserted(res);

        // when
        res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                  getDs().createUpdateOperations(Circle.class).min("radius", 5D), true);

        // then
        assertUpdated(res, 1);

        final Circle updatedCircle = getDs().get(Circle.class, id);
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(originalValue));
    }

    @Test
    public void testMinUsesSuppliedValueWhenThisIsSmallerThanCurrentDocumentValue() throws Exception {
        checkMinServerVersion(2.6);
        // given
        final ObjectId id = new ObjectId();
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 3D), true);

        assertInserted(res);

        // when
        final double newLowerValue = 2D;
        res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                  getDs().createUpdateOperations(Circle.class).min("radius", newLowerValue), true);

        // then
        assertUpdated(res, 1);

        final Circle updatedCircle = getDs().get(Circle.class, id);
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(newLowerValue));
    }

    @Test
    public void testRemoveFirst() throws Exception {
        final ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        ContainsIntArray cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values.length, is(3));
        assertThat(cIALoaded.values, is((new ContainsIntArray()).values));

        //remove 1
        UpdateResults res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                                getDs().createUpdateOperations(ContainsIntArray.class).removeFirst("values"));
        assertUpdated(res, 1);
        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{2, 3}));

        //remove 3
        res = getDs().updateFirst(getDs().createQuery(ContainsIntArray.class),
                                  getDs().createUpdateOperations(ContainsIntArray.class).removeLast("values"));
        assertUpdated(res, 1);
        cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values, is(new Integer[]{2}));
    }

    @Test
    public void testSetOnInsertWhenInserting() throws Exception {
        checkMinServerVersion(2.4);
        ObjectId id = new ObjectId();
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 2D), true);

        assertInserted(res);

        final Circle updatedCircle = getDs().get(Circle.class, id);

        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(2D));
    }

    @Test
    public void testSetOnInsertWhenUpdating() throws Exception {
        checkMinServerVersion(2.4);
        ObjectId id = new ObjectId();
        UpdateResults res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                                getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 1D), true);

        assertInserted(res);

        res = getDs().updateFirst(getDs().createQuery(Circle.class).field("id").equal(id),
                                  getDs().createUpdateOperations(Circle.class).setOnInsert("radius", 2D), true);

        assertUpdated(res, 1);

        final Circle updatedCircle = getDs().get(Circle.class, id);

        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(1D));
    }

    @Test
    public void testSetUnset() throws Exception {
        final Key<Circle> key = getDs().save(new Circle(1));

        UpdateResults res = getDs().updateFirst(getDs().find(Circle.class, "radius", 1D),
                                                getDs().createUpdateOperations(Circle.class).set("radius", 2D));

        assertUpdated(res, 1);

        final Circle c = getDs().getByKey(Circle.class, key);
        assertThat(c.getRadius(), is(2D));


        res = getDs().updateFirst(getDs().find(Circle.class, "radius", 2D), getDs().createUpdateOperations(Circle.class).unset("radius"));
        assertUpdated(res, 1);

        final Circle c2 = getDs().getByKey(Circle.class, key);
        assertThat(c2.getRadius(), is(0D));
    }

    @Test
    public void testUpdateAll() {
        getMorphia().map(EntityLogs.class, EntityLog.class);
        String uuid = "4ec6ada9-081a-424f-bee0-934c0bc4fab7";

        EntityLogs logs = new EntityLogs();
        logs.uuid = uuid;
        getDs().save(logs);

        Query<EntityLogs> finder = getDs().find(EntityLogs.class).field("uuid").equal(uuid);

        // both of these entries will have a className attribute
        List<EntityLog> latestLogs = Arrays.asList(new EntityLog("whatever1"), new EntityLog("whatever2"));
        UpdateOperations<EntityLogs> updateOperationsAll = getDs().createUpdateOperations(EntityLogs.class)
                                                                  .addAll("logs", latestLogs, false);
        getDs().update(finder, updateOperationsAll, true);
        validateNoClassName(finder.get());

        // this entry will NOT have a className attribute
        UpdateOperations<EntityLogs> updateOperations3 = getDs().createUpdateOperations(EntityLogs.class)
                                                                .add("logs", new EntityLog("whatever3"), false);
        getDs().update(finder, updateOperations3, true);
        validateNoClassName(finder.get());

        // this entry will NOT have a className attribute
        UpdateOperations<EntityLogs> updateOperations4 = getDs().createUpdateOperations(EntityLogs.class)
                                                                .add("logs", new EntityLog("whatever4"), false);
        getDs().update(finder, updateOperations4, true);
        validateNoClassName(finder.get());
    }

    @Test
    public void testUpdateFirstNoCreate() {
        getDs().delete(getDs().createQuery(EntityLogs.class));
        List<EntityLogs> logs = new ArrayList<EntityLogs>();
        for (int i = 0; i < 100; i++) {
            logs.add(createEntryLogs("name", "logs" + i));
        }
        EntityLogs logs1 = logs.get(0);
        Query<EntityLogs> query = getDs().createQuery(EntityLogs.class);
        UpdateOperations<EntityLogs> updateOperations = getDs().createUpdateOperations(EntityLogs.class);
        BasicDBObject object = new BasicDBObject("new", "value");
        updateOperations.set("raw", object);

        getDs().updateFirst(query, updateOperations, false);

        List<EntityLogs> list = getDs().createQuery(EntityLogs.class).asList();
        for (int i = 0; i < list.size(); i++) {
            final EntityLogs entityLogs = list.get(i);
            assertEquals(entityLogs.id.equals(logs1.id) ? object : logs.get(i).raw, entityLogs.raw);
        }
    }

    @Test
    public void testUpdateFirstNoCreateWithEntity() {
        List<EntityLogs> logs = new ArrayList<EntityLogs>();
        for (int i = 0; i < 100; i++) {
            logs.add(createEntryLogs("name", "logs" + i));
        }
        EntityLogs logs1 = logs.get(0);

        Query<EntityLogs> query = getDs().createQuery(EntityLogs.class);
        BasicDBObject object = new BasicDBObject("new", "value");
        EntityLogs newLogs = new EntityLogs();
        newLogs.raw = object;

        getDs().updateFirst(query, newLogs, false);

        List<EntityLogs> list = getDs().createQuery(EntityLogs.class).asList();
        for (int i = 0; i < list.size(); i++) {
            final EntityLogs entityLogs = list.get(i);
            assertEquals(entityLogs.id.equals(logs1.id) ? object : logs.get(i).raw, entityLogs.raw);
        }
    }

    @Test
    public void testUpdateFirstNoCreateWithWriteConcern() {
        List<EntityLogs> logs = new ArrayList<EntityLogs>();
        for (int i = 0; i < 100; i++) {
            logs.add(createEntryLogs("name", "logs" + i));
        }
        EntityLogs logs1 = logs.get(0);

        Query<EntityLogs> query = getDs().createQuery(EntityLogs.class);
        UpdateOperations<EntityLogs> updateOperations = getDs().createUpdateOperations(EntityLogs.class);
        BasicDBObject object = new BasicDBObject("new", "value");
        updateOperations.set("raw", object);

        getDs().updateFirst(query, updateOperations, false, WriteConcern.FSYNCED);

        List<EntityLogs> list = getDs().createQuery(EntityLogs.class).asList();
        for (int i = 0; i < list.size(); i++) {
            final EntityLogs entityLogs = list.get(i);
            assertEquals(entityLogs.id.equals(logs1.id) ? object : logs.get(i).raw, entityLogs.raw);
        }
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
        final UpdateResults res = getDs().updateFirst(getDs().find(ContainsPicKey.class, "name", cpk.name),
                                                      getDs().createUpdateOperations(ContainsPicKey.class).set("pic", pic));

        assertThat(res.getUpdatedCount(), is(1));

        //test reading the object.
        final ContainsPicKey cpk2 = getDs().find(ContainsPicKey.class).get();
        assertThat(cpk2, is(notNullValue()));
        assertThat(cpk.name, is(cpk2.name));
        assertThat(cpk2.pic, is(notNullValue()));
        assertThat(picKey, is(cpk2.pic));

        getDs().updateFirst(getDs().find(ContainsPicKey.class, "name", cpk.name),
                            getDs().createUpdateOperations(ContainsPicKey.class).set("pic", picKey));

        //test reading the object.
        final ContainsPicKey cpk3 = getDs().find(ContainsPicKey.class).get();
        assertThat(cpk3, is(notNullValue()));
        assertThat(cpk.name, is(cpk3.name));
        assertThat(cpk3.pic, is(notNullValue()));
        assertThat(picKey, is(cpk3.pic));
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
        final UpdateResults res = getDs().updateFirst(getDs().find(ContainsPic.class, "name", cp.getName()),
                                                      getDs().createUpdateOperations(ContainsPic.class).set("pic", pic));

        assertThat(res.getUpdatedCount(), is(1));

        //test reading the object.
        final ContainsPic cp2 = getDs().find(ContainsPic.class).get();
        assertThat(cp2, is(notNullValue()));
        assertThat(cp.getName(), is(cp2.getName()));
        assertThat(cp2.getPic(), is(notNullValue()));
        assertThat(cp2.getPic().getName(), is(notNullValue()));
        assertThat(pic.getName(), is(cp2.getPic().getName()));

        getDs().updateFirst(getDs().find(ContainsPic.class, "name", cp.getName()),
                            getDs().createUpdateOperations(ContainsPic.class).set("pic", picKey));

        //test reading the object.
        final ContainsPic cp3 = getDs().find(ContainsPic.class).get();
        assertThat(cp3, is(notNullValue()));
        assertThat(cp.getName(), is(cp3.getName()));
        assertThat(cp3.getPic(), is(notNullValue()));
        assertThat(cp3.getPic().getName(), is(notNullValue()));
        assertThat(pic.getName(), is(cp3.getPic().getName()));
    }

    @Test
    public void testUpdateWithDifferentType() throws Exception {
        final ContainsInt cInt = new ContainsInt();
        cInt.val = 21;
        getDs().save(cInt);

        final UpdateResults res = getDs().updateFirst(getDs().createQuery(ContainsInt.class),
                                                      getDs().createUpdateOperations(ContainsInt.class).inc("val", 1.1D));
        assertUpdated(res, 1);

        assertThat(getDs().find(ContainsInt.class).get().val, is(22));
    }

    @Test(expected = ValidationException.class)
    public void testValidationBadFieldName() throws Exception {
        getDs().update(getDs().createQuery(Circle.class).field("radius").equal(0),
                       getDs().createUpdateOperations(Circle.class).inc("r", 1D),
                       true, WriteConcern.SAFE);
    }

    private void assertInserted(final UpdateResults res) {
        assertThat(res.getInsertedCount(), is(1));
        assertThat(res.getUpdatedCount(), is(0));
        assertThat(res.getUpdatedExisting(), is(false));
    }

    private void assertUpdated(final UpdateResults res, final int count) {
        assertThat(res.getInsertedCount(), is(0));
        assertThat(res.getUpdatedCount(), is(count));
        assertThat(res.getUpdatedExisting(), is(true));
    }

    private EntityLogs createEntryLogs(final String key, final String value) {
        EntityLogs logs = new EntityLogs();
        logs.raw = new BasicDBObject(key, value);
        getDs().save(logs);

        return logs;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void validateNoClassName(final EntityLogs loaded) {
        List<DBObject> logs = (List<DBObject>) loaded.raw.get("logs");
        for (DBObject o : logs) {
            Assert.assertNull(o.get("className"));
        }
    }

    private static class ContainsIntArray {
        private final Integer[] values = {1, 2, 3};
        @Id
        private ObjectId id;
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

    @Entity(noClassnameStored = true)
    public static class EntityLogs {
        @Id
        private ObjectId id;
        @Indexed
        private String uuid;
        @Embedded
        private List<EntityLog> logs = new ArrayList<EntityLog>();
        private DBObject raw;

        @PreLoad
        public void preload(final DBObject raw) {
            this.raw = raw;
        }
    }

    @Embedded
    public static class EntityLog {
        private Date receivedTs;
        private String value;
        private DBObject raw;

        public EntityLog() {
        }

        public EntityLog(final String value) {
            this.value = value;
        }

        @PrePersist
        public void pickReceivedTs() {
            receivedTs = new Date();
        }

        @PreLoad
        public void preload(final DBObject raw) {
            this.raw = raw;
        }
    }

    private static final class Parent {
        @Embedded
        private final Set<Child> children = new HashSet<Child>();
        @Id
        private ObjectId id;
    }

    private static final class Child {
        private String first;
        private String last;

        private Child(final String first, final String last) {
            this.first = first;
            this.last = last;
        }

        private Child() {
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (last != null ? last.hashCode() : 0);
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

            Child child = (Child) o;

            if (first != null ? !first.equals(child.first) : child.first != null) {
                return false;
            }
            if (last != null ? !last.equals(child.last) : child.last != null) {
                return false;
            }

            return true;
        }
    }
}
