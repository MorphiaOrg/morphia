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

package dev.morphia.test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PreLoad;
import dev.morphia.internal.PathTarget;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Operations;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.ValidationException;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.CurrentDateOperator.TypeSpecification;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.Circle;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.Hotel;
import dev.morphia.test.models.Rectangle;
import dev.morphia.test.models.Shape;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.User;
import dev.morphia.test.query.TestQuery.CappedPic;
import dev.morphia.test.query.TestQuery.ContainsPic;
import dev.morphia.test.query.TestQuery.Pic;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.SystemVariables.NOW;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.regex;
import static dev.morphia.query.updates.UpdateOperators.addToSet;
import static dev.morphia.query.updates.UpdateOperators.and;
import static dev.morphia.query.updates.UpdateOperators.currentDate;
import static dev.morphia.query.updates.UpdateOperators.dec;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.max;
import static dev.morphia.query.updates.UpdateOperators.min;
import static dev.morphia.query.updates.UpdateOperators.mul;
import static dev.morphia.query.updates.UpdateOperators.or;
import static dev.morphia.query.updates.UpdateOperators.pop;
import static dev.morphia.query.updates.UpdateOperators.pull;
import static dev.morphia.query.updates.UpdateOperators.pullAll;
import static dev.morphia.query.updates.UpdateOperators.push;
import static dev.morphia.query.updates.UpdateOperators.rename;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.setOnInsert;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static dev.morphia.query.updates.UpdateOperators.xor;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasItem;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@SuppressWarnings({ "ConstantConditions", "unused", "removal" })
public class TestUpdateOperations extends TestBase {

    private static final JsonWriterSettings WRITER_SETTINGS = JsonWriterSettings.builder().indent(true).build();

    public TestUpdateOperations() {
        super(buildConfig(LogHolder.class, CappedPic.class, Shape.class));
    }

    @Test
    public void retainsClassName() {
        final MapsOfStuff mapsOfStuff = new MapsOfStuff();

        final Stuff1 stuff1 = new Stuff1();
        stuff1.foo = "narf";
        mapsOfStuff.map.put("k1", stuff1);

        final Stuff2 stuff2 = new Stuff2();
        stuff2.bar = "blarg";
        mapsOfStuff.map.put("k2", stuff2);

        getDs().save(mapsOfStuff);

        final Query<MapsOfStuff> query = getDs().find(MapsOfStuff.class);
        query.update(set("map.k2", stuff1));

        // fails due to type now missing
        getDs().find(MapsOfStuff.class).iterator(new FindOptions().limit(1))
                .next();
    }

    @Test(description = "see https://github.com/MorphiaOrg/morphia/issues/2472 for details")
    public void testUpdateWithDocumentConversion() {
        getDs().find(Hotel.class).filter(eq("_id", ObjectId.get()))
                .disableValidation()
                .update(
                        set("last_updated", LocalDateTime.now()),
                        push("logs", List.of(Map.of("1", 1L))),
                        push("user_detail", List.of(new FacebookUser())));
    }

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
                .filter(eq("_id", parentId),
                        eq("children.first", childName));
        UpdateResult updateResult = query.update(set("children.$.last", updatedLastName));

        // then
        assertThat(updateResult.getModifiedCount(), is(1L));
        assertThat(getDs().find(Parent.class)
                .filter(eq("id", parentId)).iterator(new FindOptions().limit(1))
                .next().children, hasItem(new Child(childName, updatedLastName)));
    }

    @Test
    public void testAdd() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);

        assertThat(get(cIntArray), is(new ContainsIntArray().values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        //add 4 to array
        assertUpdated(query.update(addToSet("values", 4)),
                1);

        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4 }));

        //add unique (4) -- noop
        assertEquals(query.update(addToSet("values", 4)).getMatchedCount(), 1);
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4 }));

        //add dup 4
        assertUpdated(query.update(push("values", 4)), 1);
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4, 4 }));

        //cleanup for next tests
        getDs().find(ContainsIntArray.class).findAndDelete();
        cIntArray = getDs().find(ContainsIntArray.class)
                .filter(eq("_id", getDs().save(new ContainsIntArray()).id))
                .first();
        assertNotNull(cIntArray);

        //add [4,5]
        final List<Integer> newValues = new ArrayList<>();
        newValues.add(4);
        newValues.add(5);

        assertUpdated(query.update(addToSet("values", newValues)), 1);
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4, 5 }));

        //add them again... noop
        assertEquals(query.update(addToSet("values", newValues)).getMatchedCount(), 1);
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4, 5 }));

        //add dups [4,5]
        assertUpdated(query.update(push("values", newValues)), 1);
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4, 5, 4, 5 }));
    }

    @Test
    public void testAddAll() {
        String uuid = "4ec6ada9-081a-424f-bee0-934c0bc4fab7";

        LogHolder logs = new LogHolder();
        logs.uuid = uuid;
        getDs().save(logs);

        Query<LogHolder> finder = getDs().find(LogHolder.class)
                .filter(eq("uuid", uuid));

        // both of these entries will have a className attribute
        List<Log> latestLogs = asList(new Log(1), new Log(2));

        finder.update(new UpdateOptions()
                .upsert(true),
                addToSet("logs", latestLogs));
        validateClassName(finder.first());

        // this entry will NOT have a className attribute
        Log log = new Log(3);
        finder
                .update(new UpdateOptions().upsert(true), addToSet("logs", log));
        validateClassName(finder.first());

        // this entry will NOT have a className attribute
        finder.update(new UpdateOptions().upsert(true), addToSet("logs", new Log(4)));
        validateClassName(finder.first());
    }

    @Test
    public void testAddToSet() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class)
                .filter(eq("_id", cIntArray.id));

        assertThat(query.first().values, is(new ContainsIntArray().values));

        assertUpdated(query.update(addToSet("values", 5)), 1);

        assertThat(query.first().values, is(new Integer[] { 1, 2, 3, 5 }));

        assertUpdated(query.update(addToSet("values", 4)), 1);
        assertThat(query.first().values, is(new Integer[] { 1, 2, 3, 5, 4 }));

        assertUpdated(query.update(addToSet("values", asList(8, 9))), 1);
        assertThat(query.first().values, is(new Integer[] { 1, 2, 3, 5, 4, 8, 9 }));

        assertEquals(query.update(addToSet("values", asList(4, 5))).getMatchedCount(), 1);
        assertThat(query.first().values, is(new Integer[] { 1, 2, 3, 5, 4, 8, 9 }));

        assertUpdated(query.update(addToSet("values", new HashSet<>(asList(10, 11)))), 1);
        assertThat(query.first().values, is(new Integer[] { 1, 2, 3, 5, 4, 8, 9, 10, 11 }));
    }

    @Test
    public void testAnd() {
        ContainsInt containsInt = new ContainsInt();
        containsInt.val = 24;

        getDs().save(containsInt);

        getDs().find(ContainsInt.class)
                .update(and("val", 8));

        ContainsInt first = getDs().find(ContainsInt.class)
                .first();

        assertEquals(first.val, 8);
    }

    @Test
    public void testCurrentDate() {
        getDs().save(new DumbColl("currentDate"));

        getDs().find(DumbColl.class)
                .update(currentDate("localDateTime"));

        Document document = getDatabase().getCollection(getDs().getCollection(DumbColl.class).getNamespace().getCollectionName())
                .find()
                .first();
        assertNotNull(document.getDate("localDateTime"));

        getDs().find(DumbColl.class)
                .update(currentDate("localDateTime")
                        .type(TypeSpecification.TIMESTAMP));

        document = getDatabase().getCollection(getDs().getCollection(DumbColl.class).getNamespace().getCollectionName())
                .find()
                .first();
        assertTrue(document.get("localDateTime") instanceof BsonTimestamp);
    }

    @Test
    public void testIncDec() {
        final Rectangle[] array = {
                new Rectangle(1, 10),
                new Rectangle(1, 10),
                new Rectangle(1, 10),
                new Rectangle(10, 10),
                new Rectangle(10, 10) };

        for (Rectangle rect : array) {
            getDs().save(rect);
        }

        final Query<Rectangle> heightOf1 = getDs().find(Rectangle.class).filter(eq("height", 1D));
        final Query<Rectangle> heightOf2 = getDs().find(Rectangle.class).filter(eq("height", 2D));
        final Query<Rectangle> heightOf35 = getDs().find(Rectangle.class).filter(eq("height", 3.5D));

        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf2.count(), is(0L));

        UpdateResult results = heightOf1
                .update(new UpdateOptions().multi(true), inc("height"));
        assertUpdated(results, 3);

        assertThat(heightOf1.count(), is(0L));
        assertThat(heightOf2.count(), is(3L));

        heightOf2.update(new UpdateOptions().multi(true), dec("height"));
        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf2.count(), is(0L));

        heightOf1.update(new UpdateOptions().multi(true), inc("height", 2.5D));
        assertThat(heightOf1.count(), is(0L));
        assertThat(heightOf35.count(), is(3L));

        heightOf35.update(new UpdateOptions().multi(true), dec("height", 2.5D));
        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf35.count(), is(0L));

        getDs().find(Rectangle.class)
                .filter(eq("height", 1D))
                .update(
                        set("height", 1D),
                        inc("width", 20D));

        MatcherAssert.assertThat(getDs().find(Rectangle.class).count(), is(5L));
        MatcherAssert.assertThat(getDs().find(Rectangle.class)
                .filter(eq("height", 1D)).iterator(new FindOptions().limit(1))
                .next(), is(notNullValue()));
        MatcherAssert.assertThat(getDs().find(Rectangle.class)
                .filter(eq("width", 30D)).iterator(new FindOptions().limit(1))
                .next(), is(notNullValue()));

        getDs().find(Rectangle.class)
                .filter(eq("width", 30D))
                .update(
                        set("height", 2D),
                        set("width", 2D));
        MatcherAssert.assertThat(getDs().find(Rectangle.class)
                .filter(eq("width", 1D)).iterator(new FindOptions().limit(1))
                .tryNext(), is(nullValue()));
        MatcherAssert.assertThat(getDs().find(Rectangle.class)
                .filter(eq("width", 2D)).iterator(new FindOptions().limit(1))
                .next(), is(notNullValue()));

        heightOf35.update(dec("height", 1));
        heightOf35.update(dec("height", Long.MAX_VALUE));
        heightOf35.update(dec("height", 1.5f));
        heightOf35.update(dec("height", Double.MAX_VALUE));
        try {
            heightOf35.update(dec("height", new AtomicInteger(1)));
            fail("Wrong data type not recognized.");
        } catch (IllegalArgumentException ignore) {
        }
    }

    @Test
    public void testElemMatchUpdate() {
        // setUp
        Object id = getDs().save(new ContainsIntArray()).id;
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", id))
                .first().values, arrayContaining(1, 2, 3));

        // do patch

        getDs().find(ContainsIntArray.class)
                .filter(eq("id", id),
                        eq("values", 2))
                .update(set("values.$", 5));

        // expected
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", id))
                .first().values, arrayContaining(1, 5, 3));
    }

    @Test
    public void testExistingUpdates() {
        getDs().save(new Circle(100D));
        getDs().save(new Circle(12D));
        Query<Circle> circle = getDs().find(Circle.class);
        assertUpdated(circle.update(inc("radius", 1D)), 1);

        assertUpdated(circle.update(new UpdateOptions().multi(true), inc("radius")), 2);

        //test possible data type change.
        final Circle updatedCircle = circle.filter(eq("radius", 13)).iterator(new FindOptions().limit(1))
                .next();
        assertThat(updatedCircle, is(notNullValue()));
        MatcherAssert.assertThat(updatedCircle.getRadius(), is(13D));
    }

    @Test
    public void testInsertWithRef() {
        final Pic pic = new Pic();
        pic.setName("fist");
        final ObjectId picKey = getDs().save(pic).getId();

        Query<ContainsPic> query = getDs().find(ContainsPic.class)
                .filter(eq("name", "first"),
                        eq("pic", picKey));
        assertInserted(query.update(new UpdateOptions().upsert(true), set("name", "A")));
        MatcherAssert.assertThat(getDs().find(ContainsPic.class).count(), is(1L));
        getDs().find(ContainsPic.class).delete(new DeleteOptions().multi(true));

        query = getDs().find(ContainsPic.class)
                .filter(eq("name", "first"),
                        eq("pic", pic));
        assertInserted(query.update(new UpdateOptions().upsert(true), set("name", "second")));
        MatcherAssert.assertThat(getDs().find(ContainsPic.class).count(), is(1L));

        //test reading the object.
        final ContainsPic cp = getDs().find(ContainsPic.class).first();
        assertThat(cp, is(notNullValue()));
        MatcherAssert.assertThat(cp.getName(), is("second"));
        MatcherAssert.assertThat(cp.getPic(), is(notNullValue()));
        MatcherAssert.assertThat(cp.getPic().getName(), is(notNullValue()));
        MatcherAssert.assertThat(cp.getPic().getName(), is("fist"));
    }

    @Test
    public void testInsertUpdate() {
        assertInserted(getDs().find(Circle.class)
                .filter(eq("radius", 0))
                .update(new UpdateOptions().upsert(true), inc("radius", 1D)));
    }

    @Test
    public void testInvalidPathsInUpdates() {
        Consumer<Datastore> test = (datastore) -> {
            Query<CappedPic> query = getDs().find(CappedPic.class);
            assertThrows(ValidationException.class, () -> query.update(max("bad.name", 12)));
            query.first();
        };

        test.accept(getDs());
        withConfig(buildConfig().legacy(), () -> {
            test.accept(getDs());
        });
    }

    @Test
    public void testInvalidPathsInModify() {
        Consumer<Datastore> test = (datastore) -> {
            Query<CappedPic> query = getDs().find(CappedPic.class);
            assertThrows(ValidationException.class, () -> query.modify(new ModifyOptions(), max("bad.name", 12)));
            query.first();
        };

        test.accept(getDs());
        withConfig(buildConfig().legacy(), () -> {
            test.accept(getDs());
        });
    }

    @Test
    public void testMax() {
        final ObjectId id = new ObjectId();
        final double originalValue = 2D;

        Datastore ds = getDs();
        Query<Circle> query = ds.find(Circle.class)
                .filter(eq("id", id));
        assertInserted(query.update(new UpdateOptions().upsert(true), setOnInsert(Map.of("radius", originalValue))));

        assertEquals(query.update(new UpdateOptions().upsert(true), max("radius", 1D))
                .getMatchedCount(), 1);

        MatcherAssert.assertThat(ds.find(Circle.class)
                .filter(eq("_id", id))
                .first().getRadius(),
                is(originalValue));
    }

    @Test
    public void testMul() {
        ContainsInt containsInt = new ContainsInt();
        containsInt.val = 2;

        getDs().save(containsInt);

        getDs().find(ContainsInt.class)
                .update(mul("val", 8));

        ContainsInt first = getDs().find(ContainsInt.class)
                .first();

        assertEquals(first.val, 16);
    }

    @Test
    public void testMaxWithDates() {
        List<User> entities = List.of(
                new User("User 1", LocalDate.of(2003, 7, 13)),
                new User("User 2", LocalDate.of(2009, 12, 1)),
                new User("User 3", LocalDate.of(2015, 8, 19)));

        getDs().save(entities);
        UpdateResult updated = getDs().find(User.class)
                .update(new UpdateOptions().multi(true), max("joined", now()));
        assertEquals(updated.getModifiedCount(), 3);

        getDs().find(User.class).delete();
        getDs().save(entities);
        updated = getDs().find(User.class)
                .update(new UpdateOptions().multi(true), max("joined", Instant.now()));
        assertEquals(updated.getModifiedCount(), 3);

        getDs().find(User.class).delete();
        getDs().save(entities);
        Calendar instance = Calendar.getInstance();
        instance.set(2136, Calendar.MAY, 13);
        Date date = instance.getTime();
        updated = getDs().find(User.class)
                .update(new UpdateOptions().multi(true), max("joined", date));
        assertEquals(updated.getModifiedCount(), 3);
    }

    @Test
    public void testMinWithDates() {
        List<User> entities = List.of(
                new User("User 1", now().minus(1, MONTHS)),
                new User("User 2", now().minus(2, MONTHS)),
                new User("User 3", now().minus(3, MONTHS)));

        getDs().save(entities);
        UpdateResult updated = getDs().find(User.class)
                .update(new UpdateOptions().multi(true), min("joined", LocalDate.of(1985, 10, 12)));
        assertEquals(updated.getModifiedCount(), 3);

        getDs().find(User.class).delete();
        getDs().save(entities);
        updated = getDs().find(User.class)
                .update(new UpdateOptions().multi(true), min("joined", Instant.now().minus(70, DAYS)));
        assertEquals(updated.getModifiedCount(), 2);

        getDs().find(User.class).delete();
        getDs().save(entities);
        Calendar instance = Calendar.getInstance();
        instance.set(86, Calendar.MAY, 13);
        Date date = instance.getTime();
        updated = getDs().find(User.class)
                .update(new UpdateOptions().multi(true), min("joined", date));
        assertEquals(updated.getModifiedCount(), 3);
    }

    @Test
    public void testMultiUpdates() {
        Query<ContainsPic> finder = getDs().find(ContainsPic.class);

        createContainsPic(0);
        createContainsPic(1);
        createContainsPic(2);

        finder.update(new UpdateOptions().multi(true), inc("size"));

        try (final MorphiaCursor<ContainsPic> iterator = finder.iterator(new FindOptions().sort(Sort.ascending("size")))) {
            for (int i = 0; i < 3; i++) {
                assertEquals(i + 1, iterator.next().getSize());
            }
        }
    }

    @Test
    public void testOr() {
        ContainsInt containsInt = new ContainsInt();
        containsInt.val = 16;

        getDs().save(containsInt);

        getDs().find(ContainsInt.class)
                .update(or("val", 8));

        ContainsInt first = getDs().find(ContainsInt.class)
                .first();

        assertEquals(first.val, 24);
    }

    @Test
    public void testPolymorphicUpsert() {
        withConfig(buildConfig()
                .enablePolymorphicQueries(true), () -> {
                    final ObjectId id = new ObjectId();
                    final double originalValue = 2D;

                    Datastore ds = getDs();
                    Query<Circle> query = ds.find(Circle.class)
                            .filter(eq("id", id));
                    assertInserted(query.update(new UpdateOptions().upsert(true), setOnInsert(Map.of("radius", originalValue))));

                    Shape first = ds.find(Shape.class).first();
                    assertNotNull(first);
                    assertTrue(first instanceof Circle);
                    assertEquals(((Circle) first).getRadius(), originalValue);
                });
    }

    @Test
    public void testPlaceholderOperators() {
        new PathTarget(getMapper(), DumbColl.class, "fromArray.$").translatedPath();
        new PathTarget(getMapper(), DumbColl.class, "fromArray.$[]").translatedPath();
        new PathTarget(getMapper(), DumbColl.class, "fromArray.$[element]").translatedPath();
    }

    @Test
    public void testPull() {
        DumbColl dumbColl = new DumbColl("ID");
        dumbColl.fromArray = List.of(new DumbArrayElement("something"), new DumbArrayElement("something else"));
        DumbColl dumbColl2 = new DumbColl("ID2");
        dumbColl2.fromArray = singletonList(new DumbArrayElement("something"));
        getDs().save(asList(dumbColl, dumbColl2));

        Query<DumbColl> query = getDs().find(DumbColl.class)
                .filter(regex("opaqueId", "ID")
                        .caseInsensitive());

        assertEquals(query.first().fromArray.size(), 2);
        query.update(pull("fromArray", Filters.eq("name", "something else")));
        assertEquals(query.first().fromArray.size(), 1);
    }

    @Test
    public void testPullsWithNoData() {
        DumbColl dumbColl = new DumbColl("ID");
        dumbColl.fromArray = singletonList(new DumbArrayElement("something"));
        DumbColl dumbColl2 = new DumbColl("ID2");
        dumbColl2.fromArray = singletonList(new DumbArrayElement("something"));
        getDs().save(asList(dumbColl, dumbColl2));

        getDs().find(DumbColl.class)
                .filter(regex("opaqueId", "ID")
                        .caseInsensitive())
                .update(pull("fromArray", Filters.eq("whereId", "not there")));

        getDs().find(DumbColl.class)
                .filter(regex("opaqueId", "ID")
                        .caseInsensitive())
                .update(pullAll("fromArray", List.of(new DumbArrayElement("something"))));
    }

    @Test
    public void testPush() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        assertThat(get(cIntArray), is(new ContainsIntArray().values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        query.update(push("values", 4));

        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4 }));

        query.update(push("values", 4));
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4, 4 }));

        query.update(push("values", asList(5, 6)));
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 3, 4, 4, 5, 6 }));

        query.update(push("values", 12)
                .position(2));

        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 12, 3, 4, 4, 5, 6 }));

        query.update(push("values", asList(99, 98, 97))
                .position(4));
        assertThat(get(cIntArray), is(new Integer[] { 1, 2, 12, 3, 99, 98, 97, 4, 4, 5, 6 }));
    }

    @Test
    public void testRemoveAllList() {
        LogHolder logs = new LogHolder();
        Date date = new Date();
        logs.logs.addAll(asList(
                new Log(1),
                new Log(2),
                new Log(3),
                new Log(1),
                new Log(2),
                new Log(3)));

        Datastore ds = getDs();
        ds.save(logs);

        UpdateResult results = ds.find(LogHolder.class)
                .update(pullAll("logs", singletonList(new Log(3))));

        assertEquals(results.getModifiedCount(), 1);
        LogHolder updated = ds.find(LogHolder.class).iterator(new FindOptions().limit(1))
                .next();
        assertEquals(updated.logs.size(), 4);
        assertTrue(updated.logs.stream()
                .allMatch(log -> log.equals(new Log(1))
                        || log.equals(new Log(2))));
    }

    @Test
    public void testRemoveFirst() {
        final ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        Integer[] values = get(cIntArray);
        assertThat(values.length, is(3));
        assertThat(values, is(new ContainsIntArray().values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        assertUpdated(query.update(pop("values").removeFirst()), 1);
        assertThat(get(cIntArray), is(new Integer[] { 2, 3 }));

        assertUpdated(query.update(pop("values")), 1);
        assertThat(get(cIntArray), is(new Integer[] { 2 }));
    }

    @Test
    public void testRename() {
        getDs().save(new DumbColl("rename"));

        getDs().find(DumbColl.class)
                .update(rename("opaqueId", "anythingElse"));

        Document document = getDatabase().getCollection(getDs().getCollection(DumbColl.class).getNamespace().getCollectionName())
                .find()
                .first();
        assertNull(document.getString("opaqueId"));
        assertNotNull(document.getString("anythingElse"));
    }

    @Test
    public void testSetOnInsertWhenInserting() {
        ObjectId id = new ObjectId();

        Query<Circle> query = getDs().find(Circle.class)
                .filter(eq("id", id));
        assertInserted(query.update(new UpdateOptions().upsert(true), setOnInsert(Map.of("radius", 2D))));

        final Circle updatedCircle = getDs().find(Circle.class)
                .filter(eq("_id", id))
                .first();

        assertThat(updatedCircle, is(notNullValue()));
        MatcherAssert.assertThat(updatedCircle.getRadius(), is(2D));
    }

    @Test
    public void testSetOnInsertWhenUpdating() {
        ObjectId id = new ObjectId();

        Query<Circle> query = getDs()
                .find(Circle.class)
                .filter(eq("id", id));

        assertInserted(query.update(new UpdateOptions().upsert(true), setOnInsert(Map.of("radius", 1D))));

        assertEquals(query.update(new UpdateOptions().upsert(true), setOnInsert(Map.of("radius", 2D)))
                .getMatchedCount(), 1);

        final Circle updatedCircle = getDs().find(Circle.class)
                .filter(eq("_id", id))
                .first();

        assertNotNull(updatedCircle);
        assertEquals(updatedCircle.getRadius(), 1D, 0.1);
    }

    @Test
    public void testSetUnset() {
        Datastore ds = getDs();
        final ObjectId key = ds.save(new Circle(1)).getId();

        Query<Circle> circle = ds.find(Circle.class)
                .filter(eq("radius", 1D));
        assertUpdated(circle.update(set("radius", 2D)), 1);

        Query<Circle> idQuery = ds.find(Circle.class)
                .filter(eq("_id", key));
        MatcherAssert.assertThat(idQuery.first().getRadius(), is(2D));

        circle = ds.find(Circle.class)
                .filter(eq("radius", 2D));
        assertUpdated(circle.update(new UpdateOptions().multi(false), unset("radius")), 1);

        MatcherAssert.assertThat(idQuery.first().getRadius(), is(0D));

        Book article = new Book();

        ds.save(article);

        assertEquals(ds.find(Book.class).update(set("title", "Some Title")).getModifiedCount(), 1);
        assertNotNull(ds.find(Book.class).filter(eq("title", "Some Title")).first());
        assertNotNull(ds.find(Book.class).filter(eq("name", "Some Title")).first());

        assertEquals(ds.find(Book.class).update(unset("name")).getModifiedCount(), 1);
        assertNull(ds.find(Book.class).filter(eq("title", "Some Title")).first());
        assertNull(ds.find(Book.class).filter(eq("name", "Some Title")).first());

        assertEquals(ds.find(Book.class).update(set("name", "Some Title")).getModifiedCount(), 1);
        assertNotNull(ds.find(Book.class).filter(eq("title", "Some Title")).first());
        assertNotNull(ds.find(Book.class).filter(eq("name", "Some Title")).first());

        assertEquals(ds.find(Book.class).update(unset("title")).getModifiedCount(), 1);
        assertNull(ds.find(Book.class).filter(eq("title", "Some Title")).first());
        assertNull(ds.find(Book.class).filter(eq("name", "Some Title")).first());
    }

    @Test
    public void testUpdateFirstNoCreate() {
        getDs().find(LogHolder.class).delete(new DeleteOptions().multi(true));
        List<LogHolder> logs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            logs.add(createEntryLogs("logs" + i));
        }
        LogHolder logs1 = logs.get(0);
        Query<LogHolder> query = getDs().find(LogHolder.class);
        Document object = new Document("new", "value");
        query.update(set("raw", object));

        List<LogHolder> list = getDs().find(LogHolder.class).iterator().toList();
        for (int i = 0; i < list.size(); i++) {
            final LogHolder logHolder = list.get(i);
            assertEquals(logHolder.id.equals(logs1.id) ? object : logs.get(i).raw, logHolder.raw);
        }
    }

    @Test
    public void testUpdateMap() {
        final Map<TestEnum, EmbeddedObjTest> map = Map.of(TestEnum.ANYVAL, new EmbeddedObjTest("name", "value"));
        getDs().find(TestMapWithEnumKey.class)
                .update(set("map", map));
    }

    @Test
    public void testUpdateRef() {
        final ContainsPic cp = new ContainsPic();
        cp.setName("cp one");

        getDs().save(cp);

        final Pic pic = new Pic();
        pic.setName("fist");
        getDs().save(pic);

        Query<ContainsPic> query = getDs().find(ContainsPic.class)
                .filter(eq("name", cp.getName()));
        UpdateResult result = query.update(set("pic", pic));
        assertEquals(result.getModifiedCount(), 1);

        //test reading the object.
        final ContainsPic cp2 = getDs().find(ContainsPic.class).iterator(new FindOptions().limit(1))
                .next();
        assertThat(cp2, is(notNullValue()));
        MatcherAssert.assertThat(cp.getName(), CoreMatchers.is(cp2.getName()));
        MatcherAssert.assertThat(cp2.getPic(), is(notNullValue()));
        MatcherAssert.assertThat(cp2.getPic().getName(), is(notNullValue()));
        MatcherAssert.assertThat(pic.getName(), CoreMatchers.is(cp2.getPic().getName()));

        //test reading the object.
        final ContainsPic cp3 = getDs().find(ContainsPic.class).iterator(new FindOptions().limit(1))
                .next();
        assertThat(cp3, is(notNullValue()));
        MatcherAssert.assertThat(cp.getName(), CoreMatchers.is(cp3.getName()));
        MatcherAssert.assertThat(cp3.getPic(), is(notNullValue()));
        MatcherAssert.assertThat(cp3.getPic().getName(), is(notNullValue()));
        MatcherAssert.assertThat(pic.getName(), CoreMatchers.is(cp3.getPic().getName()));
    }

    @Test
    public void testUpdateWithDifferentType() {
        final ContainsInt cInt = new ContainsInt();
        cInt.val = 21;
        getDs().save(cInt);

        Query<ContainsInt> query = getDs().find(ContainsInt.class);

        final UpdateResult res = query.update(inc("val", 1.1D));
        assertUpdated(res, 1);

        assertEquals(query.iterator(new FindOptions()
                .limit(1))
                .next().val, 22);
    }

    @Test
    public void testUpdateWithStages(/* MapperOptions options */) {
        checkMinServerVersion(4.2);
        getDs().save(List.of(
                new Student(1, 95, 92, 90, LocalDate.of(2020, Month.JANUARY, 5)),
                new Student(2, 98, 100, 102, LocalDate.of(2020, Month.JANUARY, 5)),
                new Student(3, 95, 110, null, LocalDate.of(2020, Month.JANUARY, 4))));

        Query<Student> query = getDs().find(Student.class)
                .filter(eq("id", 3));
        assertNull(query.first().test3);

        query.update(set()
                .field("test3", literal(98))
                .field("modified", NOW));

        assertEquals(query.first().test3, 98);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testValidationBadFieldName() {
        Query<Circle> query = getDs().find(Circle.class)
                .filter(eq("radius", 0));
        query.update(inc("rad", 1D));
    }

    @Test
    public void testValidation() {
        getDs().getCollection(Circle.class).drop();
        Query<Circle> query = getDs().find(Circle.class)
                .disableValidation()
                .filter(eq("radius", 0));
        query.update(new UpdateOptions().upsert(true),
                inc("rad", 1D));

        Document shapes = getDs().find("shapes", Document.class)
                .first();
        assertTrue(shapes.containsKey("rad"));
    }

    @Test
    public void testUpsert() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        ContainsIntArray control = new ContainsIntArray();
        getDs().save(asList(cIntArray, control));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);

        doUpdates(cIntArray, control, query.update(addToSet("values", 4)),
                new Integer[] { 1, 2, 3, 4 });

        doUpdates(cIntArray, control, query.update(addToSet("values", asList(4, 5))),
                new Integer[] { 1, 2, 3, 4, 5 });

        assertInserted(getDs().find(ContainsIntArray.class)
                .filter(eq("values", new Integer[] { 4, 5, 7 }))
                .update(new UpdateOptions().upsert(true), addToSet("values", 6)));

        query = getDs().find(ContainsIntArray.class)
                .filter(eq("values", new Integer[] { 4, 5, 7, 6 }));
        FindOptions options = new FindOptions()
                .logQuery();
        assertNotNull(query.first(options), query.getLoggedQuery());
    }

    @Test
    public void testXor() {
        ContainsInt containsInt = new ContainsInt();
        containsInt.val = 24;

        getDs().save(containsInt);

        getDs().find(ContainsInt.class)
                .update(xor("val", 8));

        ContainsInt first = getDs().find(ContainsInt.class)
                .first();

        assertEquals(first.val, 16);
    }

    private void assertInserted(UpdateResult res) {
        assertNotNull(res.getUpsertedId());
        assertEquals(res.getModifiedCount(), 0);
    }

    private void assertUpdated(UpdateResult res, long count) {
        assertEquals(count, res.getModifiedCount());
    }

    @SuppressWarnings("rawtypes")
    private void doUpdates(ContainsIntArray updated, ContainsIntArray control, UpdateResult result, Integer[] target) {
        assertUpdated(result, 1);
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", updated.id))
                .first().values,
                is(target));
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", control.id))
                .first().values,
                is(new Integer[] { 1, 2, 3 }));

        assertEquals(result.getMatchedCount(), 1);
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", updated.id))
                .first().values,
                is(target));
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", control.id))
                .first().values,
                is(new Integer[] { 1, 2, 3 }));
    }

    private void createContainsPic(int size) {
        final ContainsPic containsPic = new ContainsPic();
        containsPic.setSize(size);
        getDs().save(containsPic);
    }

    private LogHolder createEntryLogs(String value) {
        LogHolder logs = new LogHolder();
        logs.raw = new Document("name", value);
        getDs().save(logs);

        return logs;
    }

    @Entity("students")
    private static class Student {
        private final Integer test1;
        private final Integer test2;
        private final Integer test3;
        private final LocalDate modified;
        @Id
        private int id;

        public Student(int id, Integer test1, Integer test2, Integer test3, LocalDate modified) {
            this.id = id;
            this.test1 = test1;
            this.test2 = test2;
            this.test3 = test3;
            this.modified = modified;
        }
    }

    private Integer[] get(ContainsIntArray array) {
        return getDs().find(ContainsIntArray.class)
                .filter(eq("_id", array.id))
                .first().values;
    }

    @SuppressWarnings({ "unchecked" })
    private void validateClassName(@Nullable LogHolder loaded) {
        assertNotNull(loaded);
        List<Document> logs = (List<Document>) loaded.raw.get("logs");
        for (Document o : logs) {
            assertNotNull(o.get(getMapper().getConfig().discriminatorKey()), o.toString());
        }
    }

    private enum TestEnum {
        ANYVAL,
        ANOTHERVAL
    }

    @Entity
    private static final class Child {
        private String first;
        private String last;

        private Child(String first, String last) {
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
        public boolean equals(Object o) {
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
    }

    @Entity
    private static class ContainsInt {
        @Id
        private ObjectId id;
        private int val;
    }

    @Entity
    private static class ContainsIntArray {
        private final Integer[] values = { 1, 2, 3 };
        @Id
        private ObjectId id;
    }

    @Entity
    private static final class DumbArrayElement {
        private String name;

        public DumbArrayElement() {
        }

        private DumbArrayElement(String name) {
            this.name = name;
        }
    }

    @Entity
    private static final class DumbColl {
        @Id
        private ObjectId id;
        private LocalDateTime localDateTime;
        private String opaqueId;
        private List<DumbArrayElement> fromArray;

        private DumbColl() {
        }

        private DumbColl(String opaqueId) {
            this.opaqueId = opaqueId;
        }
    }

    @Entity
    private static class EmbeddedObjTest {
        private String name;
        private String value;

        public EmbeddedObjTest() {
        }

        public EmbeddedObjTest(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    @Entity
    public static class Log {
        private long receivedTs;
        private String value;

        public Log() {
        }

        public Log(long value) {
            this.value = "Log" + value;
            receivedTs = value;
        }

        @Override
        public int hashCode() {
            int result = (int) (receivedTs ^ (receivedTs >>> 32));
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Log)) {
                return false;
            }

            final Log log = (Log) o;

            if (receivedTs != log.receivedTs) {
                return false;
            }
            return value.equals(log.value);
        }

        @Override
        public String toString() {
            return format("EntityLog{receivedTs=%s, value='%s'}", receivedTs, value);
        }
    }

    @Entity(useDiscriminator = false)
    private static class LogHolder {
        @Id
        private ObjectId id;
        @Indexed
        private String uuid;
        private Log log;
        private List<Log> logs = new ArrayList<>();
        private Document raw;

        public Log getLog() {
            return log;
        }

        public void setLog(Log log) {
            this.log = log;
        }

        public List<Log> getLogs() {
            return logs;
        }

        public void setLogs(List<Log> logs) {
            this.logs = logs;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
            result = 31 * result + (log != null ? log.hashCode() : 0);
            result = 31 * result + (logs != null ? logs.hashCode() : 0);
            result = 31 * result + (raw != null ? raw.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LogHolder)) {
                return false;
            }

            final LogHolder logHolder = (LogHolder) o;

            if (!Objects.equals(id, logHolder.id)) {
                return false;
            }
            if (!Objects.equals(uuid, logHolder.uuid)) {
                return false;
            }
            if (!Objects.equals(log, logHolder.log)) {
                return false;
            }
            if (!Objects.equals(logs, logHolder.logs)) {
                return false;
            }
            return Objects.equals(raw, logHolder.raw);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", LogHolder.class.getSimpleName() + "[", "]")
                    .add("id=" + id)
                    .add("uuid='" + uuid + "'")
                    .add("log=" + log)
                    .add("logs=" + logs)
                    .add("raw=" + raw)
                    .toString();
        }

        @PreLoad
        public void preload(Document raw) {
            this.raw = raw;
        }
    }

    @Entity
    public static class MapsOfStuff {
        @Id
        private ObjectId id;
        private final Map<String, TestEntity> map = new HashMap<>();

    }

    @Entity
    private static final class Parent {
        @Id
        private ObjectId id;
        private final Set<Child> children = new HashSet<>();
    }

    public static class Stuff1 extends TestEntity {
        private String foo;
    }

    public static class Stuff2 extends TestEntity {
        private String bar;
    }

    @Entity
    private static class TestMapWithEnumKey {
        @Id
        private ObjectId id;
        private Map<TestEnum, EmbeddedObjTest> map;

    }

    @DataProvider
    private static Iterator<TranslationParams> paths() {
        List<TranslationParams> list = new ArrayList<>();

        list.addAll(prepParams("stars", "s"));
        list.addAll(prepParams("s", "s"));

        list.addAll(prepParams("address.street", "addr.address_street"));
        list.addAll(prepParams("address.address_street", "addr.address_street"));

        list.addAll(prepParams("addr.street", "addr.address_street"));
        list.addAll(prepParams("addr.address_street", "addr.address_street"));

        return list.iterator();
    }

    @NotNull
    private static List<TranslationParams> prepParams(String updateName, String mappedName) {
        LocalTime now = LocalTime.of(15, 16, 17);
        Date date = new Date(107, 14, 15);
        return List.of(
                new TranslationParams(updateName, mappedName, addToSet(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, addToSet(updateName, List.of("MARKER"))),
                new TranslationParams(updateName, mappedName, and(updateName, 42)),
                new TranslationParams(updateName, mappedName, currentDate(updateName)),
                new TranslationParams(updateName, mappedName, dec(updateName)),
                new TranslationParams(updateName, mappedName, dec(updateName, 42)),
                new TranslationParams(updateName, mappedName, inc(updateName)),
                new TranslationParams(updateName, mappedName, inc(updateName, 42)),
                new TranslationParams(updateName, mappedName, max(updateName, 42)),
                new TranslationParams(updateName, mappedName, max(updateName, now)),
                new TranslationParams(updateName, mappedName, max(updateName, date)),
                new TranslationParams(updateName, mappedName, min(updateName, 42)),
                new TranslationParams(updateName, mappedName, min(updateName, now)),
                new TranslationParams(updateName, mappedName, min(updateName, date)),
                new TranslationParams(updateName, mappedName, mul(updateName, 42)),
                new TranslationParams(updateName, mappedName, or(updateName, 42)),
                new TranslationParams(updateName, mappedName, pop(updateName)),
                new TranslationParams(updateName, mappedName, pull(updateName, eq("name", "MARKER"))),
                new TranslationParams(updateName, mappedName, pullAll(updateName, List.of("MARKER"))),
                new TranslationParams(updateName, mappedName, push(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, push(updateName, List.of("MARKER"))),
                new TranslationParams(updateName, mappedName, rename(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, set(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, setOnInsert(Map.of(mappedName, "MARKER"))),
                new TranslationParams(updateName, mappedName, unset(updateName)),
                new TranslationParams(updateName, mappedName, xor(updateName, 42)));
    }

    public static class TranslationParams {
        private String updateName;
        private String mappedName;
        final UpdateOperator operator;

        public TranslationParams(String updateName, String mappedName, UpdateOperator operator) {
            this.updateName = updateName;
            this.mappedName = mappedName;
            this.operator = operator;
        }
    }

    @Test(dataProvider = "paths")
    public void testPathTranslations(TranslationParams params) {
        CodecRegistry registry = getDs().getCodecRegistry();
        Operations value = new Operations(getMapper().getEntityModel(Hotel.class), List.of(params.operator), false);

        var json = value.toDocument(getDs())
                .toJson(WRITER_SETTINGS, registry.get(Document.class));

        String format = format("\"%s\"", params.mappedName);
        assertTrue(json.contains(format), format("failed to find '%s' in:%n%s", format, json));
    }

}
