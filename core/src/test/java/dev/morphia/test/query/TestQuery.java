package dev.morphia.test.query;

import com.jayway.awaitility.Awaitility;
import com.mongodb.CursorType;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.Key;
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.query.ArraySlice;
import dev.morphia.query.CountOptions;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.FindOptions;
import dev.morphia.query.LegacyQueryFactory;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.ValidationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.City;
import dev.morphia.test.models.CustomId;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.Keys;
import dev.morphia.test.models.Rectangle;
import dev.morphia.test.models.Student;
import dev.morphia.test.models.UsesCustomIdObject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Collation.builder;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.Sort.naturalAscending;
import static dev.morphia.query.Sort.naturalDescending;
import static dev.morphia.query.experimental.filters.Filters.and;
import static dev.morphia.query.experimental.filters.Filters.elemMatch;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static dev.morphia.query.experimental.filters.Filters.in;
import static dev.morphia.query.experimental.filters.Filters.lt;
import static dev.morphia.query.experimental.filters.Filters.ne;
import static dev.morphia.query.experimental.filters.Filters.or;
import static dev.morphia.query.experimental.filters.Filters.regex;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.bson.Document.parse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@SuppressWarnings({"unchecked", "unused"})
public class TestQuery extends TestBase {

    @Test
    public void genericMultiKeyValueQueries() {
        getMapper().map(GenericKeyValue.class);
        getDs().ensureIndexes(GenericKeyValue.class);
        final GenericKeyValue<String> value = new GenericKeyValue<>();
        final List<Object> keys = Arrays.asList("key1", "key2");
        value.key = keys;
        getDs().save(value);

        Query<GenericKeyValue> query = getDs()
                                           .find(GenericKeyValue.class)
                                           .filter(in("key", keys));
        FindOptions options = new FindOptions()
                                  .logQuery();
        final GenericKeyValue<String> found = query.iterator(options)
                                                   .tryNext();
        String loggedQuery = getDs().getLoggedQuery(options);
        assertTrue(loggedQuery.contains("{\"$in\": [\"key1\", \"key2\"]"), loggedQuery);
        assertEquals(found.id, value.id);
    }

    @Test
    public void testStreams() {
        getMapper().map(City.class);
        installData();

        List<String> list = getDs().find(City.class)
                                   .stream(new FindOptions().limit(50))
                                   .map(City::getName)
                                   .collect(Collectors.toList());
        assertEquals(list.size(), 50);

        int sum = getDs().find(City.class)
                         .stream()
                         .mapToInt(c -> 1)
                         .sum();

        assertTrue(sum > 0, sum + "");
    }

    @Test
    public void multiKeyValueQueries() {
        getMapper().map(List.of(KeyValue.class));
        getDs().ensureIndexes(KeyValue.class);
        final KeyValue value = new KeyValue();
        final List<Object> keys = Arrays.asList("key1", "key2");
        value.key = keys;
        getDs().save(value);

        FindOptions options = new FindOptions().logQuery();
        final Query<KeyValue> query = getDs().find(KeyValue.class)
                                             .filter(in("key", keys));
        query.iterator(options);
        String loggedQuery = getDs().getLoggedQuery(options);
        assertTrue(loggedQuery.contains("{\"$in\": [\"key1\", \"key2\"]"), loggedQuery);
        assertEquals(query.iterator(new FindOptions().limit(1))
                          .tryNext()
                         .id, value.id);
    }

    @Test
    public void referenceKeys() {
        final ReferenceKey key1 = new ReferenceKey("key1");

        getDs().save(asList(key1, new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4")));

        final ReferenceKeyValue value = new ReferenceKeyValue();
        value.id = key1;

        final ReferenceKeyValue key = getDs().save(value);

        final ReferenceKeyValue byKey = getDs().find(ReferenceKeyValue.class)
                                               .filter(eq("_id", key.id))
                                               .first();
        assertEquals(value.id, byKey.id);
    }

    @Test
    public void testAliasedFieldSort() {
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10), new Rectangle(10, 1)));

        Rectangle r1 = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                                  .sort(ascending("w"))
                                                                  .limit(1))
                              .tryNext();
        assertNotNull(r1);
        assertEquals(r1.getWidth(), 1, 0);

        r1 = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                        .sort(descending("w"))
                                                        .limit(1))
                    .tryNext();
        assertNotNull(r1);
        assertEquals(r1.getWidth(), 10, 0);
    }

    @Test
    public void testAlternateCollections() {
        getDs().save(new Photo(List.of("i", "am", "keywords")));

        getDs().getCollection(Photo.class)
               .renameCollection(new MongoNamespace(getDatabase().getName(), "alternate"));
        assertEquals(getDs().find(Photo.class).count(), 0);

        assertEquals(getDs().find("alternate", Photo.class).count(), 1);
    }

    @Test
    public void testCaseVariants() {
        getDs().save(asList(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4")));

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("PIC"))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("PIC")
                                           .options("i"))
                            .count(), 4);

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("PIC1"))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("PIC1")
                                           .options("i"))
                            .count(), 1);

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("C1$"))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("C1$")
                                           .options("i"))
                            .count(), 1);

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("^PIC"))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("^PIC")
                                           .options("i"))
                            .count(), 4);
    }

    @Test
    public void testCaseVariantsWithSpecialChars() {
        getDs().save(asList(
            new Pic("making waves:  _.~\"~._.~\"~._.~\"~._.~\"~._"),
            new Pic(">++('>   fish bones"),
            new Pic("hacksaw [|^^^^^^^")));

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile(quote("^"))))
                            .count(), 1);

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile(quote("aw [|^^"))))
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile(quote("AW [|^^"))))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile(quote("aw [|^^")))
                                           .options("i"))
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile(quote("AW [|^^")))
                                           .options("i"))
                            .count(), 1);

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile("^" + quote(">++('>   fish"))))
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile("^" + quote(">++('>   FIsh"))))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(compile("^" + quote(">++('>   FISH")))
                                           .options("i"))
                            .count(), 1);

        assertEquals(getDs().find(Pic.class)
                            .filter(eq("name", ">++('>   fish bones"))
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(eq("name", ">++('>   FISH BONES"))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name").pattern(quote(">++('>   fish bones")))
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern("^" + quote(">++('>   FISH BONES") + "$")
                                           .options("i"))
                            .count(), 1);

        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(quote(">++('>   fish bones") + "$"))
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(quote("'>   FISH BONES") + "$"))
                            .count(), 0);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(quote("'>   fish bones") + "$")
                                           .caseInsensitive())
                            .count(), 1);
        assertEquals(getDs().find(Pic.class)
                            .filter(regex("name")
                                           .pattern(quote("'>   FISH BONES") + "$")
                                           .caseInsensitive())
                            .count(), 1);
    }

    @Test
    public void testCollations() {
        getMapper().map(ContainsRenamedFields.class);
        getDs().save(asList(new ContainsRenamedFields("first", "last"),
            new ContainsRenamedFields("First", "Last")));

        Query query = getDs().find(ContainsRenamedFields.class)
                             .filter(eq("last_name", "last"));
        assertEquals(query.iterator().toList().size(), 1);
        assertEquals(query.iterator(new FindOptions()
                                           .collation(builder()
                                                          .locale("en")
                                                          .collationStrength(CollationStrength.SECONDARY)
                                                          .build()))
                          .toList()
                          .size(), 2);
        assertEquals(query.count(), 1);
        assertEquals(query.count(new CountOptions()
                                        .collation(builder()
                                                       .locale("en")
                                                       .collationStrength(CollationStrength.SECONDARY)
                                                       .build())), 2);
    }

    @Test
    public void testCombinationQuery() {
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4)));

        Query<Rectangle> q = getDs().find(Rectangle.class);
        q.filter(
            and(
                eq("width", 10),
                eq("height", 1)));
        FindOptions options = new FindOptions()
                                  .logQuery();
        List<Rectangle> list = q.iterator(options)
                                .toList();
        String loggedQuery = getDs().getLoggedQuery(options);
        assertEquals(q.count(), 1);

        q = getDs().find(Rectangle.class);
        q.filter(
            or(eq("width", 10), eq("height", 10)));
        assertEquals(q.count(), 3);

        q = getDs().find(Rectangle.class);
        q.filter(
            or(eq("width", 10),
                and(eq("width", 5),
                    eq("height", 8))));
        assertEquals(q.count(), 3);
    }

    @Test
    public void testCommentsShowUpInLogs() {
        getDs().save(asList(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4")));

        getDatabase().runCommand(new Document("profile", 2));
        String expectedComment = "test comment";

        getDs().find(Pic.class).iterator(new FindOptions()
                                             .comment(expectedComment))
               .toList();

        MongoCollection<Document> profileCollection = getDatabase().getCollection("system.profile");
        assertNotEquals(profileCollection.countDocuments(), 0);

        Document query = new Document("op", "query")
            .append("ns", getDs().getCollection(Pic.class).getNamespace().getFullName())
                             .append("command.comment", new Document("$exists", true));
        Document profileRecord = profileCollection.find(query).first();

        assertEquals(getCommentFromProfileRecord(profileRecord), expectedComment,
            profileRecord.toJson(getDs().getCodecRegistry().get(Document.class)));
    }

    @Test
    public void testComplexElemMatchQuery() {
        Keyword oscar = new Keyword("Oscar", 42);
        getDs().save(new PhotoWithKeywords(oscar, new Keyword("Jim", 12)));
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(elemMatch("keywords",
                              eq("keyword", "Oscar"),
                              eq("score", 12))).iterator(new FindOptions().limit(1))
                          .tryNext());

        List<PhotoWithKeywords> keywords = getDs().find(PhotoWithKeywords.class)
                                                  .filter(elemMatch("keywords",
                                                      gt("score", 20),
                                                      lt("score", 100))).iterator().toList();
        assertEquals(keywords.size(), 1);
        assertEquals(oscar, keywords.get(0).keywords.get(0));
    }

    @Test
    public void testComplexIdQuery() {
        final CustomId cId = new CustomId();
        cId.setId(new ObjectId());
        cId.setType("banker");

        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.setId(cId);
        object.setText("hllo");
        getDs().save(object);

        assertNotNull(getDs().find(UsesCustomIdObject.class)
                             .filter(eq("_id.type", "banker")).iterator(new FindOptions().limit(1))
                             .tryNext());

        assertNotNull(getDs().find(UsesCustomIdObject.class)
                             .filter(in("_id", singletonList(cId))).iterator(new FindOptions().limit(1))
                             .tryNext());
    }

    @Test
    public void testComplexIdQueryWithRenamedField() {
        final CustomId cId = new CustomId();
        cId.setId(new ObjectId());
        cId.setType("banker");

        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.setId(cId);
        object.setText("hllo");
        getDs().save(object);

        assertNotNull(getDs().find(UsesCustomIdObject.class)
                             .filter(eq("_id.t", "banker")).iterator(new FindOptions().limit(1))
                             .tryNext());
    }

    @Test
    public void testComplexRangeQuery() {
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4)));

        assertEquals(getDs().find(Rectangle.class)
                            .filter(gt("height", 3),
                                   lt("height", 8))
                            .count(), 2);
        assertEquals(getDs().find(Rectangle.class)
                            .filter(gt("height", 3),
                                   lt("height", 8),
                                   eq("width", 10))
                            .count(), 1);
    }

    @Test
    public void testCompoundSort() {
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10), new Rectangle(10, 1)));

        Rectangle r1 = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                                  .sort(ascending("width"), descending("height"))
                                                                  .limit(1))
                              .tryNext();
        assertNotNull(r1);
        assertEquals(r1.getWidth(), 1, 0);
        assertEquals(r1.getHeight(), 10, 0);

        r1 = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                        .sort(descending("height"), descending("width"))
                                                        .limit(1))
                    .tryNext();
        assertNotNull(r1);
        assertEquals(r1.getWidth(), 10, 0);
        assertEquals(r1.getHeight(), 10, 0);
    }

    @Test
    public void testCriteriaContainers() {
        try {
            check(new DefaultQueryFactory().createQuery(getDs(), User.class).disableValidation());
            fail("These operations are not supported on the modern query operation and should have failed.");
        } catch (UnsupportedOperationException e) {
            // success
        }
        check(new LegacyQueryFactory().createQuery(getDs(), User.class).disableValidation());
    }

    @Test
    public void testDeepQuery() {
        getDs().save(new PhotoWithKeywords(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona")));
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .filter(eq("keywords.keyword", "california")).iterator(new FindOptions().limit(1))
                             .tryNext());
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(eq("keywords.keyword", "not")).iterator(new FindOptions().limit(1))
                          .tryNext());
    }

    @Test
    public void testDeepQueryWithBadArgs() {
        getDs().save(new PhotoWithKeywords(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona")));
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(eq("keywords.keyword", 1)).iterator(new FindOptions().limit(1))
                          .tryNext());
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(eq("keywords.keyword", "california".getBytes())).iterator(new FindOptions().limit(1))
                          .tryNext());
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(eq("keywords.keyword", null)).iterator(new FindOptions().limit(1))
                          .tryNext());
    }

    @Test
    public void testDeepQueryWithRenamedFields() {
        getDs().save(new PhotoWithKeywords(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona")));
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .filter(eq("keywords.keyword", "california")).iterator(new FindOptions().limit(1))
                             .tryNext());
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(eq("keywords.keyword", "not")).iterator(new FindOptions().limit(1))
                          .tryNext());
    }

    @Test
    public void testDeleteQuery() {
        getDs().save(asList(new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(10, 10),
            new Rectangle(10, 10)));

        assertEquals(getDs().find(Rectangle.class).count(), 5);
        getDs().find(Rectangle.class)
               .filter(eq("height", 1))
               .delete(new DeleteOptions()
                           .multi(true));
        assertEquals(getDs().find(Rectangle.class).count(), 2);
    }

    @Test
    public void testElemMatchQuery() {
        getDs().save(asList(new PhotoWithKeywords(), new PhotoWithKeywords("Scott", "Joe", "Sarah")));
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .filter(elemMatch("keywords", eq("keyword", "Scott"))).iterator(new FindOptions().limit(1))
                             .tryNext());
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(elemMatch("keywords", eq("keyword", "Randy"))).iterator(new FindOptions().limit(1))
                          .tryNext());
    }

    @Test
    public void testElemMatchVariants() {
        final PhotoWithKeywords pwk1 = new PhotoWithKeywords();
        final PhotoWithKeywords pwk2 = new PhotoWithKeywords("Kevin");
        final PhotoWithKeywords pwk3 = new PhotoWithKeywords("Scott", "Joe", "Sarah");
        final PhotoWithKeywords pwk4 = new PhotoWithKeywords(new Keyword("Scott", 14));

        getDs().save(asList(pwk1, pwk2, pwk3, pwk4));

        assertListEquals(asList(pwk3, pwk4), getDs().find(PhotoWithKeywords.class)
                                                    .filter(elemMatch("keywords", eq("keyword", "Scott"))).iterator());

        assertListEquals(singletonList(pwk4), getDs().find(PhotoWithKeywords.class)
                                                     .filter(elemMatch("keywords", eq("score", 14))).iterator());

        assertListEquals(asList(pwk1, pwk2), getDs().find(PhotoWithKeywords.class)
                                                    .filter(elemMatch("keywords", eq("keyword", "Scott"))
                                                                .not()).iterator());
    }

    @Test
    public void testFetchKeys() {
        PhotoWithKeywords pwk1 = new PhotoWithKeywords("california", "nevada", "arizona");
        PhotoWithKeywords pwk2 = new PhotoWithKeywords("Joe", "Sarah");
        PhotoWithKeywords pwk3 = new PhotoWithKeywords("MongoDB", "World");
        getDs().save(asList(pwk1, pwk2, pwk3));

        MongoCursor<Key<PhotoWithKeywords>> keys = getDs().find(PhotoWithKeywords.class).keys();
        assertTrue(keys.hasNext());
        assertEquals(keys.next().getId(), pwk1.id);
        assertEquals(keys.next().getId(), pwk2.id);
        assertEquals(keys.next().getId(), pwk3.id);
    }

    @Test
    public void testFluentAndOrQuery() {
        getDs().save(new PhotoWithKeywords("scott", "hernandez"));

        final Query<PhotoWithKeywords> q = getDs().find(PhotoWithKeywords.class);
        q.filter(
            and(
                or(eq("keywords.keyword", "scott")),
                or(eq("keywords.keyword", "hernandez"))));

        assertEquals(q.count(), 1);
    }

    @Test
    public void testFluentNotQuery() {
        checkMinServerVersion(4.0);
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final Query<PhotoWithKeywords> query = getDs().find(PhotoWithKeywords.class);
        query.filter(
            regex("keywords.keyword").pattern("^ralph").not());

        FindOptions options = new FindOptions().logQuery();
        query.iterator();
        assertEquals(query.count(), 1);
    }

    @Test
    public void testFluentOrQuery() {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final Query<PhotoWithKeywords> q =
            getDs().find(PhotoWithKeywords.class)
                   .filter(or(
                       eq("keywords.keyword", "scott"),
                       eq("keywords.keyword", "ralph")));

        assertEquals(q.count(), 1);
    }

    @Test
    public void testIdFieldNameQuery() {
        getDs().save(new PhotoWithKeywords("scott", "hernandez"));

        assertNotNull(getDs().find(PhotoWithKeywords.class).filter(ne("id", "scott")).iterator(new FindOptions().limit(1))
                             .next());
        assertNotNull(getDs().find(PhotoWithKeywords.class).filter(eq("id", "scott").not()).iterator(new FindOptions().limit(1))
                             .next());
    }

    @Test
    public void testIdRangeQuery() {
        getDs().save(asList(new HasIntId(1), new HasIntId(11), new HasIntId(12)));
        Query<HasIntId> filter = getDs().find(HasIntId.class)
                                        .filter(gt("_id", 5),
                                            lt("_id", 20));

        FindOptions options = new FindOptions().logQuery();
        MorphiaCursor<HasIntId> list = filter.iterator(options);
        String loggedQuery = getDs().getLoggedQuery(options);
        assertEquals(filter
                            .count(), 2);
        assertEquals(getDs().find(HasIntId.class)
                            .filter(gt("_id", 0),
                                   lt("_id", 11))
                            .count(), 1);
    }

    @Test
    public void testInQuery() {
        getDs().save(new Photo(asList("red", "green", "blue")));

        assertNotNull(getDs()
                          .find(Photo.class)
                          .filter(in("keywords", asList("red", "yellow"))).iterator(new FindOptions().limit(1)).next());
    }

    @Test
    public void testInQueryWithObjects() {
        getDs().save(asList(new PhotoWithKeywords(), new PhotoWithKeywords("Scott", "Joe", "Sarah")));

        final Query<PhotoWithKeywords> query = getDs()
                                                   .find(PhotoWithKeywords.class)
                                                   .filter(in("keywords", asList(new Keyword("Scott"), new Keyword("Randy"))));
        assertNotNull(query.iterator(new FindOptions().limit(1)).next());
    }

    @Test
    public void testKeyList() {
        final Rectangle rect = new Rectangle(1000, 1);

        Rectangle rectangle = getDs().save(rect);
        assertEquals(rectangle.getId(), rect.getId());

        final FacebookUser fbUser1 = new FacebookUser(1, "scott");
        final FacebookUser fbUser2 = new FacebookUser(2, "tom");
        final FacebookUser fbUser3 = new FacebookUser(3, "oli");
        final FacebookUser fbUser4 = new FacebookUser(4, "frank");
        final List<FacebookUser> users = getDs().save(asList(fbUser1, fbUser2, fbUser3, fbUser4));
        assertEquals(fbUser1.getId(), 1);

        final List<Key<FacebookUser>> fbUserKeys = new ArrayList<>();
        for (FacebookUser user : users) {
            fbUserKeys.add(getMapper().getKey(user));
        }

        assertEquals(fbUserKeys.get(0).getId(), fbUser1.getId());
        assertEquals(fbUserKeys.get(1).getId(), fbUser2.getId());
        assertEquals(fbUserKeys.get(2).getId(), fbUser3.getId());
        assertEquals(fbUserKeys.get(3).getId(), fbUser4.getId());

        final Keys k1 = new Keys(getMapper().getKey(rectangle), fbUserKeys);
        final Keys keys = getDs().save(k1);
        assertEquals(k1.getId(), keys.getId());

        final Datastore datastore = getDs();

        final Keys k1Loaded = datastore.find(Keys.class)
                                       .filter(eq("_id", k1.getId()))
                                       .first();
        for (Key<FacebookUser> key : k1Loaded.getUsers()) {
            assertNotNull(key.getId());
        }

        assertNotNull(k1Loaded.getRect().getId());
    }

    @Test
    public void testKeys() {
        PhotoWithKeywords pwk1 = new PhotoWithKeywords("california", "nevada", "arizona");
        PhotoWithKeywords pwk2 = new PhotoWithKeywords("Joe", "Sarah");
        PhotoWithKeywords pwk3 = new PhotoWithKeywords("MongoDB", "World");
        getDs().save(asList(pwk1, pwk2, pwk3));

        MongoCursor<Key<PhotoWithKeywords>> keys = getDs()
                                                       .find(PhotoWithKeywords.class)
                                                       .keys();
        assertTrue(keys.hasNext());
        assertEquals(pwk1.id, keys.next().getId());
        assertEquals(pwk2.id, keys.next().getId());
        assertEquals(pwk3.id, keys.next().getId());

        List<UsesCustomIdObject> list = asList(new UsesCustomIdObject(new CustomId("Turk"), "Turk"),
            new UsesCustomIdObject(new CustomId("JD"), "Dorian"),
            new UsesCustomIdObject(new CustomId("Carla"), "Espinosa"));
        getDs().save(list);

        Iterator<Key<UsesCustomIdObject>> complexKeys = getDs().find(UsesCustomIdObject.class).keys();
        assertTrue(complexKeys.hasNext());
        assertEquals(list.get(0).getId(), complexKeys.next().getId());
        assertEquals(list.get(1).getId(), complexKeys.next().getId());
        assertEquals(list.get(2).getId(), complexKeys.next().getId());
        assertFalse(complexKeys.hasNext());
    }

    @Test
    public void testMixedProjection() {
        getDs().save(new ContainsRenamedFields("Frank", "Zappa"));

        try {
            getDs().find(ContainsRenamedFields.class)
                   .iterator(new FindOptions()
                                 .projection().include("first_name")
                                 .projection().exclude("last_name"));
            fail("An exception should have been thrown indication a mixed projection");
        } catch (ValidationException e) {
            // all good
        }

        try {
            getDs().find(ContainsRenamedFields.class)
                   .iterator(new FindOptions()
                                 .projection().include("first_name", "last_name")
                                 .projection().exclude("_id"));
        } catch (ValidationException e) {
            fail("An exception should not have been thrown indication a mixed projection because _id suppression is a special case");
        }

        try {
            getDs().find(ContainsRenamedFields.class)
                   .iterator(new FindOptions()
                                 .projection().exclude("first_name", "last_name")
                                 .projection().include("_id"));
            fail("An exception should have been thrown indication a mixed projection");
        } catch (ValidationException e) {
            // all good
        }

        try {
            getDs().find(IntVector.class)
                   .iterator(new FindOptions()
                                 .projection().exclude("name")
                                 .projection().project("scalars", new ArraySlice(5)));
            fail("An exception should have been thrown indication a mixed projection");
        } catch (ValidationException e) {
            // all good
        }
    }

    @Test
    public void testMultipleConstraintsOnOneField() {
        getMapper().map(ContainsPic.class);
        getDs().ensureIndexes();
        Query<ContainsPic> query = getDs().find(ContainsPic.class);
        query.filter(gte("size", 10),
            lt("size", 100));

        Map<String, Object> explain = query.explain();
        Map<String, Object> queryPlanner = (Map<String, Object>) explain.get("queryPlanner");
        Map<String, Object> winningPlan = (Map<String, Object>) queryPlanner.get("winningPlan");
        Map<String, Object> inputStage = (Map<String, Object>) winningPlan.get("inputStage");
        assertEquals(inputStage.get("stage"), "IXSCAN");
    }

    @Test
    public void testNaturalSortAscending() {
        getDs().save(asList(new Rectangle(6, 10), new Rectangle(3, 8), new Rectangle(10, 10), new Rectangle(10, 1)));

        List<Rectangle> results = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                                             .sort(naturalAscending()))
                                         .toList();

        assertEquals(results.size(), 4);

        Rectangle r;

        r = results.get(0);
        assertNotNull(r);
        assertEquals(r.getHeight(), 6, 0);
        assertEquals(r.getWidth(), 10, 0);

        r = results.get(1);
        assertNotNull(r);
        assertEquals(r.getHeight(), 3, 0);
        assertEquals(r.getWidth(), 8, 0);

        r = results.get(2);
        assertNotNull(r);
        assertEquals(r.getHeight(), 10, 0);
        assertEquals(r.getWidth(), 10, 0);
    }

    @Test
    public void testNaturalSortDescending() {
        getDs().save(asList(new Rectangle(6, 10), new Rectangle(3, 8), new Rectangle(10, 10), new Rectangle(10, 1)));

        List<Rectangle> results = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                                             .sort(naturalDescending()))
                                         .toList();

        assertEquals(results.size(), 4);

        Rectangle r;

        r = results.get(0);
        assertNotNull(r);
        assertEquals(r.getHeight(), 10, 0);
        assertEquals(r.getWidth(), 1, 0);

        r = results.get(1);
        assertNotNull(r);
        assertEquals(r.getHeight(), 10, 0);
        assertEquals(r.getWidth(), 10, 0);

        r = results.get(2);
        assertNotNull(r);
        assertEquals(r.getHeight(), 3, 0);
        assertEquals(r.getWidth(), 8, 0);
    }

    @Test
    public void testNegativeBatchSize() {
        getDs().find(PhotoWithKeywords.class).delete(new DeleteOptions().multi(true));
        getDs().save(asList(new PhotoWithKeywords("scott", "hernandez"),
            new PhotoWithKeywords("scott", "hernandez"),
            new PhotoWithKeywords("scott", "hernandez"),
            new PhotoWithKeywords("1", "2"),
            new PhotoWithKeywords("3", "4"),
            new PhotoWithKeywords("5", "6")));
        assertEquals(getDs().find(PhotoWithKeywords.class).iterator(new FindOptions()
                                                                           .batchSize(-2)).toList()
                            .size(), 2);
    }

    @Test
    public void testNoLifeCycleEventsOnParameters() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic("some pic");
        getDs().save(p);
        cpk.setPic(p);
        getDs().save(cpk);

        Pic queryPic = new Pic("some pic");
        queryPic.setId(p.getId());
        Query query = getDs().find(ContainsPic.class)
                             .filter(eq("pic", queryPic));
        assertFalse(queryPic.isPrePersist());
        assertNotNull(query.iterator(new FindOptions().limit(1))
                           .tryNext());

        getDs().find(ContainsPic.class)
               .filter(elemMatch("pic", eq("pic", queryPic)));
        assertFalse(queryPic.isPrePersist());
    }

    @Test
    public void testNonexistentFindGet() {
        assertNull(getDs().find(Student.class).filter(eq("_id", -1)).iterator(new FindOptions().limit(1))
                          .tryNext());
    }

    @Test
    public void testNonexistentGet() {
        assertNull(getDs().find(Student.class)
                          .filter(eq("_id", -1))
                          .first());
    }

    @Test
    public void testProject() {
        getDs().save(new ContainsRenamedFields("Frank", "Zappa"));

        ContainsRenamedFields found = getDs().find(ContainsRenamedFields.class).iterator(new FindOptions()
                                                                                             .projection().include("first_name")
                                                                                             .limit(1))
                                             .tryNext();
        assertNotNull(found.firstName);
        assertNull(found.lastName);

        found = getDs().find(ContainsRenamedFields.class).iterator(new FindOptions()
                                                                       .projection().include("first_name")
                                                                       .limit(1))
                       .tryNext();
        assertNotNull(found.firstName);
        assertNull(found.lastName);

        try {
            getDs()
                .find(ContainsRenamedFields.class).iterator(new FindOptions()
                                                                .projection().include("bad field name")
                                                                .limit(1))
                .tryNext();
            fail("Validation should have caught the bad field");
        } catch (ValidationException e) {
            // success!
        }

        Query<ContainsRenamedFields> query = getDs().find(ContainsRenamedFields.class);
    }

    @Test
    public void testProjectArrayField() {
        int[] ints = {0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
        IntVector vector = new IntVector(ints);
        getDs().save(vector);

        assertEquals(copy(ints, 0, 4),
            getDs().find(IntVector.class).iterator(new FindOptions()
                                                       .projection().project("scalars", new ArraySlice(4))
                                                       .limit(1))
                   .next()
                .scalars);
        assertEquals(copy(ints, 5, 4), getDs().find(IntVector.class).iterator(new FindOptions()
                                                                                       .projection()
                                                                                       .project("scalars", new ArraySlice(5, 4))
                                                                                       .limit(1))
                                                          .next()
                                                .scalars);
        assertEquals(copy(ints, ints.length - 10, 6),
            getDs().find(IntVector.class).iterator(new FindOptions()
                                                       .projection().project("scalars", new ArraySlice(-10, 6))
                                                       .limit(1))
                   .next()
                .scalars);
        assertEquals(copy(ints, ints.length - 12, 12),
            getDs().find(IntVector.class).iterator(new FindOptions()
                                                       .projection().project("scalars", new ArraySlice(-12))
                                                       .limit(1))
                   .next()
                .scalars);
    }

    @Test
    public void testQBE() {
        final CustomId cId = new CustomId();
        cId.setId(new ObjectId());
        cId.setType("banker");

        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.setId(cId);
        object.setText("hllo");
        getDs().save(object);
        final UsesCustomIdObject loaded;

        // Add back if/when query by example for embedded fields is supported (require dotting each field).
        // CustomId exId = new CustomId();
        // exId.type = cId.type;
        // loaded = getDs().find(UsesCustomIdObject.class, "_id", exId).get();
        // assertNotNull(loaded);

        final UsesCustomIdObject ex = new UsesCustomIdObject();
        ex.setText(object.getText());
        loaded = getDs().queryByExample(ex).iterator(new FindOptions().limit(1))
                        .next();
        assertNotNull(loaded);
    }

    @Test
    public void testQueryCount() {
        getDs().save(asList(new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(10, 10),
            new Rectangle(10, 10)));

        assertEquals(getDs().find(Rectangle.class)
                            .filter(eq("height", 1D))
                            .count(), 3);
        assertEquals(getDs().find(Rectangle.class)
                            .filter(eq("height", 10D))
                            .count(), 2);
        assertEquals(getDs().find(Rectangle.class)
                            .filter(eq("width", 10D))
                            .count(), 5);

    }

    @Test
    public void testQueryOverLazyReference() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        cpk.lazyPic = p;

        getDs().save(cpk);

        assertEquals(getDs().find(ContainsPic.class)
                            .filter(eq("lazyPic", p))
                            .count(), 1);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testQueryOverReference() {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        cpk.pic = p;

        getDs().save(cpk);

        final Query<ContainsPic> query = getDs().find(ContainsPic.class);

        Query<ContainsPic> pic = query.filter(eq("pic", p));
        assertEquals(pic.count(), 1);

        getDs().find(ContainsPic.class)
               .filter(eq("pic.name", "foo"))
               .first();
    }

    @Test
    public void testQueryUnmappedData() {
        getMapper().map(Class1.class);
        getDs().ensureIndexes();

        getDs().getDatabase().getCollection("user").insertOne(
            new Document()
                .append("@class", Class1.class.getName())
                .append("value1", "foo")
                .append("someMap", new Document("someKey", "value")));

        Query<Class1> query = getDs().find(Class1.class);
        query.disableValidation().filter(eq("someMap.someKey", "value"));
        Class1 retrievedValue = query.iterator(new FindOptions().limit(1)).next();
        assertNotNull(retrievedValue);
        assertEquals(retrievedValue.value1, "foo");
    }

    @Test
    public void testRangeQuery() {
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4)));

        assertEquals(getDs().find(Rectangle.class)
                            .filter(gt("height", 3))
                            .count(), 4);
        assertEquals(getDs().find(Rectangle.class)
                            .filter(gt("height", 3),
                                   lt("height", 10))
                            .count(), 3);
        assertEquals(getDs().find(Rectangle.class)
                            .filter(gt("height", 9),
                                   lt("width", 5))
                            .count(), 1);
        assertEquals(getDs().find(Rectangle.class)
                            .filter(lt("height", 7))
                            .count(), 3);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testReferenceQuery() {
        final Photo p = new Photo();
        final HasPhotoReference cpk = new HasPhotoReference();
        cpk.photo = getDs().save(p);
        getDs().save(cpk);

        Query<HasPhotoReference> query = getDs().find(HasPhotoReference.class)
                                                .filter(eq("photo", p));
        FindOptions options = new FindOptions()
                                  .logQuery()
                                  .limit(1);
        HasPhotoReference photoKey = query.iterator(options)
                                          .tryNext();

        assertNotNull(photoKey, getDs().getLoggedQuery(options));

        assertNotNull(getDs().find(HasPhotoReference.class)
                             .filter(eq("photo", cpk.photo)).iterator(new FindOptions()
                                                                          .limit(1))
                             .tryNext());
        assertNull(getDs().find(HasPhotoReference.class)
                          .filter(eq("photo", 1)).iterator(new FindOptions()
                                                               .limit(1))
                          .tryNext());

        getDs().find(HasPhotoReference.class)
               .filter(eq("photo.keywords", "foo")).iterator(new FindOptions()
                                                                 .limit(1))
               .next();
    }

    @Test
    public void testRenamedFieldQuery() {
        getDs().save(new ContainsRenamedFields("Scott", "Bakula"));

        assertNotNull(getDs().find(ContainsRenamedFields.class)
                             .filter(eq("firstName", "Scott")).iterator(new FindOptions().limit(1))
                             .next());
        assertNotNull(getDs().find(ContainsRenamedFields.class)
                             .filter(eq("first_name", "Scott")).iterator(new FindOptions().limit(1))
                             .next());
    }

    @Test
    public void testRetrievedFields() {
        getDs().save(new ContainsRenamedFields("Frank", "Zappa"));

        ContainsRenamedFields found = getDs()
                                          .find(ContainsRenamedFields.class).iterator(new FindOptions()
                                                                                          .projection().include("first_name")
                                                                                          .limit(1))
                                          .tryNext();
        assertNotNull(found.firstName);
        assertNull(found.lastName);

        found = getDs()
                    .find(ContainsRenamedFields.class).iterator(new FindOptions()
                                                                    .projection().include("firstName")
                                                                    .limit(1))
                    .tryNext();
        assertNotNull(found.firstName);
        assertNull(found.lastName);

        try {
            getDs()
                .find(ContainsRenamedFields.class).iterator(new FindOptions()
                                                                .projection().include("bad field name")
                                                                .limit(1))
                .tryNext();
            fail("Validation should have caught the bad field");
        } catch (ValidationException e) {
            // success!
        }
    }

    @Test
    public void testReturnOnlyIndexedFields() {
        getMapper().map(Pic.class);
        getDs().ensureIndexes(Pic.class);
        getDs().save(asList(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4")));

        Pic foundItem = getDs().find(Pic.class)
                               .filter(eq("name", "pic2"))
                               .first(new FindOptions()
                                          .limit(1)
                                          .returnKey(true));
        assertNotNull(foundItem);
        assertThat("Name should be populated", foundItem.getName(), is("pic2"));
        assertNull(foundItem.getId(), "ID should not be populated");
    }

    @Test
    public void testSimpleSort() {
        getMapper().map(Rectangle.class);
        getDs().ensureIndexes();
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10), new Rectangle(10, 1)));

        Rectangle r1 = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                                  .sort(ascending("width"))
                                                                  .limit(1))
                              .next();
        assertNotNull(r1);
        assertEquals(r1.getWidth(), 1, 0);

        r1 = getDs().find(Rectangle.class).iterator(new FindOptions()
                                                        .sort(descending("width"))
                                                        .limit(1))
                    .next();
        assertNotNull(r1);
        assertEquals(r1.getWidth(), 10, 0);
    }

    @Test
    public void testTailableCursors() {
        getMapper().map(CappedPic.class);
        final Datastore ds = getDs();
        final Query<CappedPic> query = ds.find(CappedPic.class);

        getMapper().map(CappedPic.class);
        ds.ensureCaps();

        final List<CappedPic> found = new ArrayList<>();
        assertCapped(CappedPic.class, 1000, 1024 * 1024);

        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        assertEquals(query.count(), 0);

        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(
            () -> ds.save(new CappedPic()), 0, 100, TimeUnit.MILLISECONDS);

        Awaitility
            .await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> getDs().find(CappedPic.class).count() > 0);

        final Iterator<CappedPic> tail = query.iterator(new FindOptions()
                                                            .cursorType(CursorType.Tailable));
        Awaitility
            .await()
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                if (tail.hasNext()) {
                    found.add(tail.next());
                }
                return found.size() >= 10;
            });
        executorService.shutdownNow();
        assertTrue(found.size() >= 10);
        assertTrue(query.count() >= 10);
    }

    @Test
    public void testThatElemMatchQueriesOnlyChecksRequiredFields() {
        final PhotoWithKeywords pwk1 = new PhotoWithKeywords(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona"));
        final PhotoWithKeywords pwk2 = new PhotoWithKeywords("Joe", "Sarah");
        pwk2.keywords.add(new Keyword("Scott", 14));

        getDs().save(asList(pwk1, pwk2));

        // In this case, we only want to match on the keyword field, not the
        // score field, which shouldn't be included in the elemMatch query.

        // As a result, the query in MongoDB should look like:
        // find({ keywords: { $elemMatch: { keyword: "Scott" } } })

        // NOT:
        // find({ keywords: { $elemMatch: { keyword: "Scott", score: 12 } } })
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .filter(elemMatch("keywords", eq("keyword", "Scott"))).iterator(new FindOptions().limit(1))
                             .tryNext());

        assertNull(getDs().find(PhotoWithKeywords.class)
                          .filter(elemMatch("keywords", eq("keyword", "Randy"))).iterator(new FindOptions().limit(1))
                          .tryNext());
    }

    private <T> void assertListEquals(List<T> list, MongoCursor<T> cursor) {
        for (T t : list) {
            assertEquals(cursor.next(), t, list.toString());
        }
    }

    @SuppressWarnings("removal")
    private void check(Query<User> query) {
        query
            .field("version").equal("latest")
            .and(
                query.or(
                    query.criteria("fieldA").equal("a"),
                    query.criteria("fieldB").equal("b")),
                query.and(
                    query.criteria("fieldC").equal("c"),
                    query.or(
                        query.criteria("fieldD").equal("d"),
                        query.criteria("fieldE").equal("e"))));

        query.and(query.criteria("fieldF").equal("f"));

        final Document queryObject = query.toDocument();

        final Document parse = parse(
            "{\"version\": \"latest\", \"$and\": [{\"$or\": [{\"fieldA\": \"a\"}, {\"fieldB\": \"b\"}]}, {\"fieldC\": \"c\", \"$or\": "
            + "[{\"fieldD\": \"d\"}, {\"fieldE\": \"e\"}]}], \"fieldF\": \"f\","
            + "\"_t\": { \"$in\" : [ \"User\"]}}");

        assertEquals(parse, queryObject);
    }

    private int[] copy(int[] array, int start, int count) {
        return copyOfRange(array, start, start + count);
    }

    private void dropProfileCollection() {
        MongoCollection<Document> profileCollection = getDatabase().getCollection("system.profile");
        profileCollection.drop();
    }

    private String getCommentFromProfileRecord(Document profileRecord) {
        if (profileRecord.containsKey("command")) {
            Document commandDocument = ((Document) profileRecord.get("command"));
            if (commandDocument.containsKey("comment")) {
                return (String) commandDocument.get("comment");
            }
        }
        if (profileRecord.containsKey("query")) {
            Document queryDocument = ((Document) profileRecord.get("query"));
            if (queryDocument.containsKey("comment")) {
                return (String) queryDocument.get("comment");
            } else if (queryDocument.containsKey("$comment")) {
                return (String) queryDocument.get("$comment");
            }
        }
        return null;
    }

    private Query<Pic> getQuery(QueryFactory queryFactory) {
        return queryFactory.createQuery(getDs(), Pic.class);
    }

    private void turnOffProfiling() {
        getDatabase().runCommand(new Document("profile", 0).append("slowms", 100));
    }

    private void turnOnProfiling() {
        getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
    }

    @Entity
    public interface User {
    }

    @Entity(value = "capped_pic", cap = @CappedAt(count = 1000))
    public static class CappedPic extends Pic {
        public CappedPic() {
            super(System.currentTimeMillis() + "");
        }
    }

    @Entity(value = "user", useDiscriminator = false)
    private static class Class1 {
        @Id
        private ObjectId id;

        private String value1;

    }

    @Entity
    public static class ContainsPic {
        @Id
        private ObjectId id;
        private String name = "test";
        @Reference
        private Pic pic;
        @Reference(lazy = true)
        private Pic lazyPic;
        @Reference(lazy = true)
        private PicWithObjectId lazyObjectIdPic;
        @Indexed
        private int size;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public PicWithObjectId getLazyObjectIdPic() {
            return lazyObjectIdPic;
        }

        public void setLazyObjectIdPic(PicWithObjectId lazyObjectIdPic) {
            this.lazyObjectIdPic = lazyObjectIdPic;
        }

        public Pic getLazyPic() {
            return lazyPic;
        }

        public void setLazyPic(Pic lazyPic) {
            this.lazyPic = lazyPic;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Pic getPic() {
            return pic;
        }

        public void setPic(Pic pic) {
            this.pic = pic;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "ContainsPic{" +
                   "id=" + id +
                   ", name='" + name + '\'' +
                   ", size=" + size +
                   '}';
        }
    }

    @Entity(useDiscriminator = false)
    public static class ContainsRenamedFields {
        @Id
        private ObjectId id;
        @Property("first_name")
        private String firstName;
        @Property("last_name")
        private String lastName;

        public ContainsRenamedFields() {
        }

        ContainsRenamedFields(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Entity
    private static class GenericKeyValue<T> {

        @Id
        private ObjectId id;

        @Indexed(options = @IndexOptions(unique = true))
        private List<Object> key;

        private T value;
    }

    @Entity
    public static class HasIntId {
        @Id
        private int id;

        protected HasIntId() {
        }

        HasIntId(int id) {
            this.id = id;
        }
    }

    @Entity
    private static class HasPhotoReference {
        @Id
        private ObjectId id;
        @Reference
        private Photo photo;
    }

    @Entity
    static class IntVector {
        @Id
        private ObjectId id;
        private String name;
        private int[] scalars;

        IntVector() {
        }

        IntVector(int... scalars) {
            this.scalars = scalars;
        }
    }

    @Entity
    private static class KeyValue {
        @Id
        private ObjectId id;
        /**
         * The list of keys for this value.
         */
        @Indexed(options = @IndexOptions(unique = true))
        private List<Object> key;
        /**
         * The id of the value document
         */
        @Indexed
        private ObjectId value;
    }

    @Entity
    public static class Keyword {
        private String keyword;
        private int score;

        protected Keyword() {
        }

        Keyword(String k) {
            this.keyword = k;
        }

        Keyword(String k, Integer score) {
            this.keyword = k;
            this.score = score;
        }

        Keyword(Integer score) {
            this.score = score;
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyword, score);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Keyword)) {
                return false;
            }
            final Keyword keyword1 = (Keyword) o;
            return score == keyword1.score &&
                   Objects.equals(keyword, keyword1.keyword);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Keyword.class.getSimpleName() + "[", "]")
                       .add("keyword='" + keyword + "'")
                       .add("score=" + score)
                       .toString();
        }
    }

    @Entity
    public static class Photo {
        @Id
        private ObjectId id;
        private List<String> keywords = singletonList("amazing");

        public Photo() {
        }

        Photo(List<String> keywords) {
            this.keywords = keywords;
        }
    }

    @Entity
    public static class PhotoWithKeywords {
        @Id
        private ObjectId id;
        private List<Keyword> keywords = new ArrayList<>();

        PhotoWithKeywords() {
        }

        PhotoWithKeywords(String... words) {
            keywords = new ArrayList<>(words.length);
            for (String word : words) {
                keywords.add(new Keyword(word));
            }
        }

        PhotoWithKeywords(Keyword... keyword) {
            keywords.addAll(asList(keyword));
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, keywords);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PhotoWithKeywords)) {
                return false;
            }
            final PhotoWithKeywords that = (PhotoWithKeywords) o;
            return id.equals(that.id) &&
                   keywords.equals(that.keywords);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PhotoWithKeywords.class.getSimpleName() + "[", "]")
                       .add("id=" + id)
                       .add("keywords=" + keywords)
                       .toString();
        }
    }

    @Entity
    public static class Pic {
        @Id
        private ObjectId id;
        @Indexed
        private String name;
        private boolean prePersist;

        public Pic() {
        }

        Pic(String name) {
            this.name = name;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getName() != null ? getName().hashCode() : 0);
            result = 31 * result + (isPrePersist() ? 1 : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pic)) {
                return false;
            }

            final Pic pic = (Pic) o;

            if (isPrePersist() != pic.isPrePersist()) {
                return false;
            }
            if (getId() != null ? !getId().equals(pic.getId()) : pic.getId() != null) {
                return false;
            }
            return getName() != null ? getName().equals(pic.getName()) : pic.getName() == null;
        }

        @PrePersist
        public void tweak() {
            prePersist = true;
        }

        boolean isPrePersist() {
            return prePersist;
        }

        public void setPrePersist(boolean prePersist) {
            this.prePersist = prePersist;
        }
    }

    @Entity
    public static class PicWithObjectId {
        @Id
        private ObjectId id;
        private String name;
    }

    private static class RectangleComparator implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o1.getWidth(), o2.getWidth());
            return compare != 0 ? compare : Double.compare(o2.getHeight(), o1.getHeight());
        }
    }

    private static class RectangleComparator1 implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o2.getHeight(), o1.getHeight());
            return compare != 0 ? compare : Double.compare(o2.getWidth(), o1.getWidth());
        }
    }

    private static class RectangleComparator2 implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o1.getWidth(), o2.getWidth());
            return compare != 0 ? compare : Double.compare(o1.getHeight(), o2.getHeight());
        }
    }

    private static class RectangleComparator3 implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o1.getWidth(), o2.getWidth());
            return compare != 0 ? compare : Double.compare(o1.getHeight(), o2.getHeight());
        }
    }

    @Entity
    static class ReferenceKey {
        @Id
        private ObjectId id;
        private String name;

        ReferenceKey() {
        }

        ReferenceKey(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ReferenceKey that = (ReferenceKey) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            return name != null ? name.equals(that.name) : that.name == null;
        }
    }

    @Entity
    private static class ReferenceKeyValue {
        @Id
        private ReferenceKey id;
        /**
         * The list of keys for this value.
         */
        @Indexed(options = @IndexOptions(unique = true))
        @Reference
        private List<Pic> key;
        /**
         * The id of the value document
         */
        @Indexed
        private ObjectId value;
    }

    static class UserImpl implements User {
        @Id
        @SuppressWarnings("unused")
        private ObjectId id;
    }
}
