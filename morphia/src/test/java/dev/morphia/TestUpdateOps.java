/*
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

package dev.morphia;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.query.Sort;
import dev.morphia.query.TestQuery.ContainsPic;
import dev.morphia.query.TestQuery.Pic;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateOpsImpl;
import dev.morphia.query.ValidationException;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.Article;
import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.Translation;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.morphia.query.PushOptions.options;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Scott Hernandez
 */
@SuppressWarnings("UnusedDeclaration")
public class TestUpdateOps extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TestUpdateOps.class);

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
        Query<Parent> query = getDs().find(Parent.class)
                                     .field("_id").equal(parentId)
                                     .field("children.first")
                                     .equal(childName);
        UpdateResult updateResult = query.update()
                                         .set("children.$.last", updatedLastName)
                                         .execute();

        // then
        assertThat(updateResult.getModifiedCount(), is(1));
        assertThat(getDs().find(Parent.class).filter("id", parentId)
                          .execute(new FindOptions().limit(1))
                          .next()
                       .children, hasItem(new Child(childName, updatedLastName)));
    }

    @Test
    public void testDisableValidation() {
        Child child1 = new Child("James", "Rigney");

        validateClassName("children", getDs().createUpdateOperations(Parent.class)
                                             .removeAll("children", child1), false);

        validateClassName("children", getDs().createUpdateOperations(Parent.class)
                                             .disableValidation()
                                             .removeAll("children", child1), false);

        validateClassName("c", getDs().createUpdateOperations(Parent.class)
                                      .disableValidation()
                                      .removeAll("c", child1), true);
    }

    private void validateClassName(final String path, final UpdateOperations<Parent> ops, final boolean expected) {
        Document ops1 = ((UpdateOpsImpl) ops).getOps();
        Map pull = (Map) ops1.get("$pull");
        Map children = (Map) pull.get(path);
        assertEquals(expected, children.containsKey("className"));
    }

    @Test
    public void testAdd() {
        checkMinServerVersion(2.6);

        ContainsIntArray cIntArray = new ContainsIntArray();
        Datastore ds = getDs();
        ds.save(cIntArray);

        assertThat(ds.get(cIntArray).values, is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = ds.createQuery(ContainsIntArray.class);
        //add 4 to array
        assertUpdated(query.update()
                           .addToSet("values", 4)
                           .execute(),
            1);

        assertThat(ds.get(cIntArray).values, is(new Integer[]{1, 2, 3, 4}));

        //add unique (4) -- noop
        assertUpdated(query.update().addToSet("values", 4).execute(), 1);
        assertThat(ds.get(cIntArray).values, is(new Integer[]{1, 2, 3, 4}));

        //add dup 4
        assertUpdated(query.update().push("values", 4).execute(), 1);
        assertThat(ds.get(cIntArray).values, is(new Integer[]{1, 2, 3, 4, 4}));

        //cleanup for next tests
        ds.delete(ds.find(ContainsIntArray.class));
        cIntArray = ds.getByKey(ContainsIntArray.class, ds.save(new ContainsIntArray()));

        //add [4,5]
        final List<Integer> newValues = new ArrayList<>();
        newValues.add(4);
        newValues.add(5);

        assertUpdated(query.update().addToSet("values", newValues).execute(), 1);
        assertThat(ds.get(cIntArray).values, is(new Integer[]{1, 2, 3, 4, 5}));

        //add them again... noop
        assertUpdated(query.update().addToSet("values", newValues).execute(), 1);
        assertThat(ds.get(cIntArray).values, is(new Integer[]{1, 2, 3, 4, 5}));

        //add dups [4,5]
        assertUpdated(query.update().push("values", newValues).execute(), 1);
        assertThat(ds.get(cIntArray).values, is(new Integer[]{1, 2, 3, 4, 5, 4, 5}));
    }

    @Test
    public void testAddAll() {
        getMapper().map(EntityLogs.class, EntityLog.class);
        String uuid = "4ec6ada9-081a-424f-bee0-934c0bc4fab7";

        EntityLogs logs = new EntityLogs();
        logs.uuid = uuid;
        getDs().save(logs);

        Query<EntityLogs> finder = getDs().find(EntityLogs.class).field("uuid").equal(uuid);

        // both of these entries will have a className attribute
        List<EntityLog> latestLogs = asList(new EntityLog("whatever1", new Date()), new EntityLog("whatever2", new Date()));

        finder.update()
              .addToSet("logs", latestLogs).execute(new UpdateOptions().upsert(true));
        validateNoClassName(finder.first());

        // this entry will NOT have a className attribute
        EntityLog log = new EntityLog("whatever3", new Date());
        finder
            .update()
            .addToSet("logs", log)
            .execute(new UpdateOptions().upsert(true));
        validateNoClassName(finder.first());

        // this entry will NOT have a className attribute
        finder.update()
              .addToSet("logs", new EntityLog("whatever4", new Date()))
              .execute(new UpdateOptions().upsert(true));
        validateNoClassName(finder.first());
    }

    @Test
    public void testMultiUpdates() {
        getMapper().map(ContainsPic.class);
        Query<ContainsPic> finder = getDs().find(ContainsPic.class);

        createContainsPic(0);
        createContainsPic(1);
        createContainsPic(2);

        finder.update()
              .inc("size")
              .execute( new UpdateOptions().multi(true));

        final MorphiaCursor<ContainsPic> iterator = finder.order(Sort.ascending("size")).execute();
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, iterator.next().getSize());
        }
    }

    public void createContainsPic(final int size) {
        final ContainsPic containsPic = new ContainsPic();
        containsPic.setSize(size);
        getDs().save(containsPic);
    }


    @Test
    public void testAddToSet() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);

        assertThat(getDs().get(cIntArray).values, is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        assertUpdated(query.update().addToSet("values", 5).execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 5}));

        assertUpdated(query.update().addToSet("values", 4).execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 5, 4}));

        assertUpdated(query.update().addToSet("values", asList(8, 9)).execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 5, 4, 8, 9}));

        assertUpdated(query.update().addToSet("values", asList(4, 5)).execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 5, 4, 8, 9}));

        assertUpdated(query.update().addToSet("values", new HashSet<>(asList(10, 11))).execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 5, 4, 8, 9, 10, 11}));
    }

    @Test
    public void testUpsert() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        ContainsIntArray control = new ContainsIntArray();
        getDs().save(asList(cIntArray, control));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);

        doUpdates(cIntArray, control, query.update().addToSet("values", 4),
                  new Integer[]{1, 2, 3, 4});


        doUpdates(cIntArray, control, query.update().addToSet("values", asList(4, 5)),
                  new Integer[]{1, 2, 3, 4, 5});


        assertInserted(getDs().find(ContainsIntArray.class)
                              .filter("values", new Integer[]{4, 5, 7})
                              .update()
                              .addToSet("values", 6)
                              .execute(new UpdateOptions().upsert(true)));

        assertNotNull(getDs().find(ContainsIntArray.class)
                             .filter("values", new Integer[]{4, 5, 7, 6})
                             .first());
    }

    private void doUpdates(final ContainsIntArray updated, final ContainsIntArray control, final Update update, final Integer[] target) {
        assertUpdated(update.execute(new UpdateOptions()), 1);
        assertThat(getDs().get(updated).values, is(target));
        assertThat(getDs().get(control).values, is(new Integer[]{1, 2, 3}));

        assertUpdated(update.execute(new UpdateOptions()), 1);
        assertThat(getDs().get(updated).values, is(target));
        assertThat(getDs().get(control).values, is(new Integer[]{1, 2, 3}));
    }

    @Test
    public void testExistingUpdates() {
        getDs().save(new Circle(100D));
        getDs().save(new Circle(12D));
        Query<Circle> circle = getDs().find(Circle.class);
        assertUpdated(circle.update().inc("radius", 1D).execute(), 1);

        assertUpdated(circle.update().inc("radius").execute(new UpdateOptions().multi(true)), 2);

        //test possible data type change.
        final Circle updatedCircle = circle.filter("radius", 13)
                                            .execute(new FindOptions().limit(1))
                                            .next();
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(13D));
    }

    @Test
    public void testIncDec() {
        final Rectangle[] array = {
            new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(10, 10),
            new Rectangle(10, 10)};

        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        final Query<Rectangle> heightOf1 = getDs().find(Rectangle.class).filter("height", 1D);
        final Query<Rectangle> heightOf2 = getDs().find(Rectangle.class).filter("height", 2D);
        final Query<Rectangle> heightOf35 = getDs().find(Rectangle.class).filter("height", 3.5D);

        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf2.count(), is(0L));

        UpdateResult results = heightOf1
                                   .update()
                                   .inc("height")
                                   .execute(new UpdateOptions().multi(true));
        assertUpdated(results, 3);

        assertThat(heightOf1.count(), is(0L));
        assertThat(heightOf2.count(), is(3L));

        heightOf2.update().dec("height").execute(new UpdateOptions().multi(true));
        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf2.count(), is(0L));

        heightOf1.update().inc("height", 2.5D).execute(new UpdateOptions().multi(true));
        assertThat(heightOf1.count(), is(0L));
        assertThat(heightOf35.count(), is(3L));

        heightOf35.update().dec("height", 2.5D).execute(new UpdateOptions().multi(true));
        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf35.count(), is(0L));

        getDs().find(Rectangle.class).filter("height", 1D)
               .update()
               .set("height", 1D)
               .inc("width", 20D)
               .execute();

        assertThat(getDs().find(Rectangle.class).count(), is(5L));
        assertThat(getDs().find(Rectangle.class).filter("height", 1D)
                          .execute(new FindOptions().limit(1))
                          .next(), is(notNullValue()));
        assertThat(getDs().find(Rectangle.class).filter("width", 30D)
                          .execute(new FindOptions().limit(1))
                          .next(), is(notNullValue()));

        getDs().find(Rectangle.class).filter("width", 30D)
               .update()
               .set("height", 2D).set("width", 2D)
               .execute();
        assertThat(getDs().find(Rectangle.class).filter("width", 1D)
                          .execute(new FindOptions().limit(1))
                          .tryNext(), is(nullValue()));
        assertThat(getDs().find(Rectangle.class).filter("width", 2D)
                          .execute(new FindOptions().limit(1))
                          .next(), is(notNullValue()));

        heightOf35.update().dec("height", 1).execute();
        heightOf35.update().dec("height", Long.MAX_VALUE).execute();
        heightOf35.update().dec("height", 1.5f).execute();
        heightOf35.update().dec("height", Double.MAX_VALUE).execute();
        try {
            heightOf35.update()
                      .dec("height", new AtomicInteger(1));
            fail("Wrong data type not recognized.");
        } catch (IllegalArgumentException ignore) {
        }
    }

    @Test
    public void testInsertUpdate() {
        assertInserted(getDs().find(Circle.class).field("radius").equal(0)
                              .update()
                              .inc("radius", 1D)
                              .execute(new UpdateOptions().upsert(true)));
    }

    @Test
    public void testInsertWithRef() {
        final Pic pic = new Pic();
        pic.setName("fist");
        final Key<Pic> picKey = getDs().save(pic);

        Query<ContainsPic> query = getDs().find(ContainsPic.class).filter("name", "first").filter("pic", picKey);
        assertInserted(query.update()
                            .set("name", "A")
                            .execute(new UpdateOptions().upsert(true)));
        assertThat(getDs().find(ContainsPic.class).count(), is(1L));
        getDs().delete(getDs().find(ContainsPic.class));

        query = getDs().find(ContainsPic.class).filter("name", "first").filter("pic", pic);
        assertInserted(query.update()
                            .set("name", "second")
                            .execute(new UpdateOptions().upsert(true)));
        assertThat(getDs().find(ContainsPic.class).count(), is(1L));

        //test reading the object.
        final ContainsPic cp = getDs().find(ContainsPic.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cp, is(notNullValue()));
        assertThat(cp.getName(), is("second"));
        assertThat(cp.getPic(), is(notNullValue()));
        assertThat(cp.getPic().getName(), is(notNullValue()));
        assertThat(cp.getPic().getName(), is("fist"));
    }

    @Test
    public void testMaxKeepsCurrentDocumentValueWhenThisIsLargerThanSuppliedValue() {
        checkMinServerVersion(2.6);
        final ObjectId id = new ObjectId();
        final double originalValue = 2D;

        Datastore ds = getDs();
        Query<Circle> query = ds.find(Circle.class)
                              .field("id").equal(id);
        assertInserted(query.update()
                            .setOnInsert("radius", originalValue)
                            .execute(new UpdateOptions().upsert(true)));

        assertUpdated(query.update()
                           .max("radius", 1D)
                           .execute(new UpdateOptions().upsert(true)), 1);

        assertThat(ds.find(Circle.class).filter("_id", id).first().getRadius(), is(originalValue));
    }

    @Test
    public void testMinKeepsCurrentDocumentValueWhenThisIsSmallerThanSuppliedValue() {
        checkMinServerVersion(2.6);
        final ObjectId id = new ObjectId();
        final double originalValue = 3D;

        Query<Circle> query = getDs().find(Circle.class).field("id").equal(id);
        assertInserted(query.update()
                            .setOnInsert("radius", originalValue)
                            .execute(new UpdateOptions().upsert(true)));

        assertUpdated(query.update()
                           .min("radius", 5D)
                           .execute(new UpdateOptions().upsert(true)), 1);

        final Circle updatedCircle = getDs().find(Circle.class).filter("_id", id).first();
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(originalValue));
    }

    @Test
    public void testMinUsesSuppliedValueWhenThisIsSmallerThanCurrentDocumentValue() {
        checkMinServerVersion(2.6);
        final ObjectId id = new ObjectId();
        final double newLowerValue = 2D;

        Query<Circle> query = getDs().find(Circle.class).field("id").equal(id);
        assertInserted(query.update()
                            .setOnInsert("radius", 3D)
                            .execute(new UpdateOptions().upsert(true)));


        assertUpdated(query.update()
                           .min("radius", newLowerValue)
                           .execute(new UpdateOptions().upsert(true)), 1);

        final Circle updatedCircle = getDs().find(Circle.class).filter("_id", id).first();
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(newLowerValue));
    }

    @Test
    public void testPush() {
        checkMinServerVersion(2.6);
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        assertThat(getDs().get(cIntArray).values, is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        query.update()
             .push("values", 4)
             .execute();

        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 4}));

        query.update()
             .push("values", 4)
             .execute();
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 4, 4}));

        query.update()
             .push("values", asList(5, 6))
             .execute();
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 3, 4, 4, 5, 6}));

        query.update()
             .push("values", 12, options().position(2))
             .execute();

        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 12, 3, 4, 4, 5, 6}));


        query.update()
             .push("values", asList(99, 98, 97), options().position(4))
             .execute();
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{1, 2, 12, 3, 99, 98, 97, 4, 4, 5, 6}));
    }

    @Test
    public void testRemoveAllSingleValue() {
        EntityLogs logs = new EntityLogs();
        Date date = new Date();
        logs.logs.addAll(asList(
            new EntityLog("log1", date),
            new EntityLog("log2", date),
            new EntityLog("log3", date),
            new EntityLog("log1", date),
            new EntityLog("log2", date),
            new EntityLog("log3", date)));

        Datastore ds = getDs();
        ds.save(logs);

        UpdateResult results = ds.find(EntityLogs.class).update()
                                  .removeAll("logs", new EntityLog("log3", date))
                                  .execute();

        assertEquals(1, results.getModifiedCount());
        EntityLogs updated = ds.find(EntityLogs.class)
                               .execute(new FindOptions().limit(1))
                               .next();
        assertEquals(4, updated.logs.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(new EntityLog("log" + ((i % 2) + 1), date), updated.logs.get(i));
        }
    }

    @Test
    public void testRemoveAllList() {
        EntityLogs logs = new EntityLogs();
        Date date = new Date();
        logs.logs.addAll(asList(
            new EntityLog("log1", date),
            new EntityLog("log2", date),
            new EntityLog("log3", date),
            new EntityLog("log1", date),
            new EntityLog("log2", date),
            new EntityLog("log3", date)));

        Datastore ds = getDs();
        ds.save(logs);

        UpdateResult results = ds.find(EntityLogs.class).update()
                                  .removeAll("logs", singletonList(new EntityLog("log3", date)))
                                  .execute();

        assertEquals(1, results.getModifiedCount());
        EntityLogs updated = ds.find(EntityLogs.class)
                               .execute(new FindOptions()
                                   .comment("morphia test query")
                                            .limit(1))
                               .next();
        assertEquals(4, updated.logs.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(new EntityLog("log" + ((i % 2) + 1), date), updated.logs.get(i));
        }
    }

    @Test
    public void testRemoveWithNoData() {
        DumbColl dumbColl = new DumbColl("ID");
        dumbColl.fromArray = singletonList(new DumbArrayElement("something"));
        DumbColl dumbColl2 = new DumbColl("ID2");
        dumbColl2.fromArray = singletonList(new DumbArrayElement("something"));
        getDs().save(asList(dumbColl, dumbColl2));

        UpdateResult deleteResults = getDs().find(DumbColl.class)
                                             .field("opaqueId").equalIgnoreCase("ID")
                                             .update(new Document("$pull",
                                               new Document("fromArray", new Document("whereId", "not there"))))
                                             .execute();

        final UpdateResult execute = getDs().find(DumbColl.class).field("opaqueId").equalIgnoreCase("ID")
                                             .update()
                                             .removeAll("fromArray", new DumbArrayElement("something"))
                                             .execute();
    }

    @Test
    public void testElemMatchUpdate() {
        // setUp
        Object id = getDs().save(new ContainsIntArray()).getId();
        assertThat(getDs().find(ContainsIntArray.class).filter("_id", id).first().values, arrayContaining(1, 2, 3));

        // do patch

        getDs().find(ContainsIntArray.class)
               .filter("id", id)
               .filter("values", 2)
               .update()
               .set("values.$", 5)
               .execute();

        // expected
        assertThat(getDs().find(ContainsIntArray.class).filter("_id", id).first().values, arrayContaining(1, 5, 3));
    }

    @Test
    public void testRemoveFirst() {
        final ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        ContainsIntArray cIALoaded = getDs().get(cIntArray);
        assertThat(cIALoaded.values.length, is(3));
        assertThat(cIALoaded.values, is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        assertUpdated(query.update().removeFirst("values").execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{2, 3}));

        assertUpdated(query.update().removeLast("values").execute(), 1);
        assertThat(getDs().get(cIntArray).values, is(new Integer[]{2}));
    }

    @Test
    public void testSetOnInsertWhenInserting() {
        checkMinServerVersion(2.4);
        ObjectId id = new ObjectId();

        Query<Circle> query = getDs().find(Circle.class).field("id").equal(id);
        assertInserted(query.update()
                            .setOnInsert("radius", 2D)
                            .execute(new UpdateOptions().upsert(true)));

        final Circle updatedCircle = getDs().find(Circle.class).filter("_id", id).first();

        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(2D));
    }

    @Test
    public void testSetOnInsertWhenUpdating() {
        checkMinServerVersion(2.4);
        ObjectId id = new ObjectId();

        Query<Circle> query = getDs().find(Circle.class).field("id").equal(id);
        assertInserted(query.update().setOnInsert("radius", 1D).execute(new UpdateOptions().upsert(true)));

        assertUpdated(query.update().setOnInsert("radius", 2D).execute(new UpdateOptions().upsert(true)), 1);
        final Circle updatedCircle = getDs().find(Circle.class).filter("_id", id).first();

        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(1D));
    }

    @Test
    public void testSetUnset() {
        Datastore ds = getDs();
        final Key<Circle> key = ds.save(new Circle(1));

        Query<Circle> circle = ds.find(Circle.class).filter("radius", 1D);
        assertUpdated(circle.update().set("radius", 2D).execute(), 1);

        assertThat(ds.getByKey(Circle.class, key).getRadius(), is(2D));

        circle = ds.find(Circle.class).filter("radius", 2D);
        assertUpdated(circle.update().unset("radius").execute(new UpdateOptions().multi(false)), 1);

        assertThat(ds.getByKey(Circle.class, key).getRadius(), is(0D));

        Article article = new Article();

        ds.save(article);

        Query<Article> query = ds.find(Article.class);
        query.update()
             .set("translations", new HashMap<String, Translation>())
             .execute();

        query.update()
             .unset("translations")
             .execute();
    }

    @Test
    public void testUpdateFirstNoCreate() {
        getDs().delete(getDs().find(EntityLogs.class));
        List<EntityLogs> logs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            logs.add(createEntryLogs("logs" + i));
        }
        EntityLogs logs1 = logs.get(0);
        Query<EntityLogs> query = getDs().find(EntityLogs.class);
        Document object = new Document("new", "value");
        query.update()
             .set("raw", object)
             .execute();

        List<EntityLogs> list = getDs().find(EntityLogs.class).execute().toList();
        for (int i = 0; i < list.size(); i++) {
            final EntityLogs entityLogs = list.get(i);
            assertEquals(entityLogs.id.equals(logs1.id) ? object : logs.get(i).raw, entityLogs.raw);
        }
    }

    @Test
    public void testUpdateKeyRef() {
        final ContainsPicKey cpk = new ContainsPicKey();
        cpk.name = "cpk one";

        Datastore ds = getDs();
        ds.save(cpk);

        final Pic pic = new Pic();
        pic.setName("fist again");
        final Key<Pic> picKey = ds.save(pic);
        // picKey = getDs().getKey(pic);


        //test with Key<Pic>

        Query<ContainsPicKey> query = ds.find(ContainsPicKey.class).filter("name", cpk.name);
        assertThat(query.update().set("pic", pic).execute().getModifiedCount(), is(1));

        //test reading the object.
        final ContainsPicKey cpk2 = ds.find(ContainsPicKey.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cpk2, is(notNullValue()));
        assertThat(cpk.name, is(cpk2.name));
        assertThat(cpk2.pic, is(notNullValue()));
        assertThat(picKey, is(cpk2.pic));

        query.update().set("pic", picKey).execute();

        //test reading the object.
        final ContainsPicKey cpk3 = ds.find(ContainsPicKey.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cpk3, is(notNullValue()));
        assertThat(cpk.name, is(cpk3.name));
        assertThat(cpk3.pic, is(notNullValue()));
        assertThat(picKey, is(cpk3.pic));
    }

    @Test
    public void testUpdateKeyList() {
        final ContainsPicKey cpk = new ContainsPicKey();
        cpk.name = "cpk one";

        Datastore ds = getDs();
        ds.save(cpk);

        final Pic pic = new Pic();
        pic.setName("fist again");
        final Key<Pic> picKey = ds.save(pic);

        cpk.keys = singletonList(picKey);

        //test with Key<Pic>
        Query<ContainsPicKey> query = ds.find(ContainsPicKey.class).filter("name", cpk.name);
        final UpdateResult res = query.update().set("keys", cpk.keys).execute();

        assertThat(res.getModifiedCount(), is(1));

        //test reading the object.
        final ContainsPicKey cpk2 = ds.find(ContainsPicKey.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cpk2, is(notNullValue()));
        assertThat(cpk.name, is(cpk2.name));
        assertThat(cpk2.keys, hasItem(picKey));
    }

    @Test
    public void testUpdateRef() {
        final ContainsPic cp = new ContainsPic();
        cp.setName("cp one");

        getDs().save(cp);

        final Pic pic = new Pic();
        pic.setName("fist");
        final Key<Pic> picKey = getDs().save(pic);


        //test with Key<Pic>

        Query<ContainsPic> query = getDs().find(ContainsPic.class).filter("name", cp.getName());
        assertThat(query.update().set("pic", pic).execute().getModifiedCount(), is(1));

        //test reading the object.
        final ContainsPic cp2 = getDs().find(ContainsPic.class)
                                       .execute(new FindOptions().limit(1))
                                       .next();
        assertThat(cp2, is(notNullValue()));
        assertThat(cp.getName(), is(cp2.getName()));
        assertThat(cp2.getPic(), is(notNullValue()));
        assertThat(cp2.getPic().getName(), is(notNullValue()));
        assertThat(pic.getName(), is(cp2.getPic().getName()));

        query.update().set("pic", picKey).execute();

        //test reading the object.
        final ContainsPic cp3 = getDs().find(ContainsPic.class)
                                       .execute(new FindOptions().limit(1))
                                       .next();
        assertThat(cp3, is(notNullValue()));
        assertThat(cp.getName(), is(cp3.getName()));
        assertThat(cp3.getPic(), is(notNullValue()));
        assertThat(cp3.getPic().getName(), is(notNullValue()));
        assertThat(pic.getName(), is(cp3.getPic().getName()));
    }

    @Test
    public void testUpdateWithDifferentType() {
        final ContainsInt cInt = new ContainsInt();
        cInt.val = 21;
        getDs().save(cInt);

        Query<ContainsInt> query = getDs().find(ContainsInt.class);
        final UpdateResult res = query.update().inc("val", 1.1D).execute();
        assertUpdated(res, 1);

        assertThat(query.execute(new FindOptions().limit(1)).next().val, is(22));
    }

    @Test(expected = ValidationException.class)
    public void testValidationBadFieldName() {
        Query<Circle> query = getDs().find(Circle.class).field("radius").equal(0);
        query.update().inc("r", 1D).execute();
    }

    private void assertInserted(final UpdateResult res) {
        assertThat(res.getUpsertedId(), is(1));
        assertThat(res.getModifiedCount(), is(0));
    }

    private void assertUpdated(final UpdateResult res, final int count) {
        assertThat(res.getModifiedCount(), is(count));
    }

    private EntityLogs createEntryLogs(final String value) {
        EntityLogs logs = new EntityLogs();
        logs.raw = new Document("name", value);
        getDs().save(logs);

        return logs;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void validateNoClassName(final EntityLogs loaded) {
        List<Document> logs = (List<Document>) loaded.raw.get("logs");
        for (Document o : logs) {
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
        private List<Key<Pic>> keys;
    }

    @Entity(useDiscriminator = false)
    public static class EntityLogs {
        @Id
        private ObjectId id;
        @Indexed
        private String uuid;
        private List<EntityLog> logs = new ArrayList<>();
        private Document raw;

//        @PreLoad
        public void preload(final Document raw) {
            this.raw = raw;
        }
    }

    @Embedded
    public static class EntityLog {
        private Date receivedTs;
        private String value;

        public EntityLog() {
        }

        EntityLog(final String value, final Date date) {
            this.value = value;
            receivedTs = date;
        }

        @Override
        public int hashCode() {
            int result = receivedTs != null ? receivedTs.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EntityLog)) {
                return false;
            }

            final EntityLog entityLog = (EntityLog) o;

            return receivedTs != null ? receivedTs.equals(entityLog.receivedTs)
                                      : entityLog.receivedTs == null && (value != null ? value.equals(entityLog.value)
                                                                                       : entityLog.value == null);

        }


        @Override
        public String toString() {
            return String.format("EntityLog{receivedTs=%s, value='%s'}", receivedTs, value);
        }
    }

    private static final class Parent {
        private final Set<Child> children = new HashSet<>();
        @Id
        private ObjectId id;
    }

    @Embedded
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
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Child)) {
                return false;
            }

            final Child child = (Child) o;

            if (!Objects.equals(first, child.first)) {
                return false;
            }
            return Objects.equals(last, child.last);
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (last != null ? last.hashCode() : 0);
            return result;
        }
    }

    private static final class DumbColl {
        private String opaqueId;
        private List<DumbArrayElement> fromArray;

        private DumbColl() {
        }

        private DumbColl(final String opaqueId) {
            this.opaqueId = opaqueId;
        }
    }

    private static final class DumbArrayElement {
        private String whereId;

        private DumbArrayElement(final String whereId) {
            this.whereId = whereId;
        }
    }
}
