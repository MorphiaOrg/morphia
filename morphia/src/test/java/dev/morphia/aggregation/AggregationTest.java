/*
 * Copyright (c) 2008 - 2013 MongoDB, Inc. <http://mongodb.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.aggregation;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BucketGranularity;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.aggregation.experimental.stages.Out;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.geo.PlaceWithLegacyCoords;
import dev.morphia.geo.model.City;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.month;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.year;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.stages.GeoNear.to;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Group.of;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
public class AggregationTest extends TestBase {

    @Test
    public void testBucketAutoWithGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5),
            new Book("Divine Comedy", "Dante", 7),
            new Book("Eclogues", "Dante", 40),
            new Book("The Odyssey", "Homer", 21)));

        Iterator<BooksBucketResult> aggregate = getDs().aggregate(Book.class)
                                                       .autoBucket(AutoBucket.of()
                                                                             .groupBy(field("copies"))
                                                                             .buckets(3)
                                                                             .granularity(BucketGranularity.POWERSOF2)
                                                                             .outputField("authors", addToSet(field("author")))
                                                                             .outputField("count", sum(value(1))))
                                                       .execute(BooksBucketResult.class);
        BooksBucketResult result1 = aggregate.next();
        assertEquals(result1.getId().min, 4);
        assertEquals(result1.getId().max, 8);
        assertEquals(result1.getCount(), 2);
        assertEquals(result1.authors, singleton("Dante"));

        result1 = aggregate.next();
        assertEquals(result1.getId().min, 8);
        assertEquals(result1.getId().max, 32);
        assertEquals(result1.getCount(), 1);
        assertEquals(result1.authors, singleton("Homer"));

        result1 = aggregate.next();
        assertEquals(result1.getId().min, 32);
        assertEquals(result1.getId().max, 64);
        assertEquals(result1.getCount(), 1);
        assertEquals(result1.authors, singleton("Dante"));
        assertFalse(aggregate.hasNext());

    }

    @Test
    public void testBucketAutoWithoutGranularity() {
        getDs().save(asList(
            new Book("The Banquet", "Dante", 5),
            new Book("Divine Comedy", "Dante", 10),
            new Book("Eclogues", "Dante", 40),
            new Book("The Odyssey", "Homer", 21)));

        Iterator<BucketAutoResult> aggregate = getDs().aggregate(Book.class)
                                                      .autoBucket(AutoBucket.of()
                                                                            .groupBy(field("copies"))
                                                                            .buckets(2))
                                                      .execute(BucketAutoResult.class);
        BucketAutoResult result1 = aggregate.next();
        assertEquals(result1.id.min, 5);
        assertEquals(result1.id.max, 21);
        assertEquals(result1.count, 2);
        result1 = aggregate.next();
        assertEquals(result1.id.min, 21);
        assertEquals(result1.id.max, 40);
        assertEquals(result1.count, 2);
        assertFalse(aggregate.hasNext());

    }

    @Test(expected = RuntimeException.class)
    public void testBucketWithBoundariesWithSizeLessThanTwo() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        getDs().aggregate(Book.class)
               .bucket(Bucket.of()
                             .groupBy(field("copies"))
                             .boundaries(value(10))
                             .outputField("count", sum(value(1))))
               .execute(BucketResult.class);
    }

    @Test
    public void testBucketWithOptions() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));


        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class)
                                                  .bucket(Bucket.of()
                                                                .groupBy(field("copies"))
                                                                .boundaries(value(1), value(5), value(10))
                                                                .defaultValue("test")
                                                                .outputField("count", sum(value(1))))
                                                  .execute(BucketResult.class);

        BucketResult result1 = aggregate.next();
        assertEquals(result1.id, "1");
        assertEquals(result1.count, 3);

        BucketResult result2 = aggregate.next();
        assertEquals(result2.id, "test");
        assertEquals(result2.count, 2);

    }

    @Test(expected = RuntimeException.class)
    public void testBucketWithUnsortedBoundaries() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class)
                                                  .bucket(Bucket.of()
                                                                .groupBy(field("copies"))
                                                                .boundaries(value(5), value(1), value(10))
                                                                .defaultValue("test")
                                                                .outputField("count", sum(value(1))))
                                                  .execute(BucketResult.class);

    }

    @Test
    public void testBucketWithoutOptions() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class)
                                                  .bucket(Bucket.of()
                                                                .groupBy(field("copies"))
                                                                .boundaries(value(1), value(5), value(12)))
                                                  .execute(BucketResult.class);
        BucketResult result1 = aggregate.next();
        assertEquals(result1.id, "1");
        assertEquals(result1.count, 3);

        BucketResult result2 = aggregate.next();
        assertEquals(result2.id, "5");
        assertEquals(result2.count, 2);
    }

    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", new Date()), new User("John Doe", new Date())));

        Query<User> query = getDs().find(User.class).filter(eq("name", "john doe"));
        Aggregation<User> pipeline = getDs().aggregate(User.class)
                                            .match(query);
        Assert.assertEquals(1, count(pipeline.execute(User.class)));

        Assert.assertEquals(2, count(pipeline.execute(User.class,
            new dev.morphia.aggregation.experimental.AggregationOptions()
                .collation(Collation.builder()
                                    .locale("en")
                                    .collationStrength(SECONDARY)
                                    .build()))));
    }

    @Test
    public void testDateAggregation() {
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .group(of(
                                             id()
                                                 .field("month", month(field("date")))
                                                 .field("year", year(field("date"))))
                                                    .field("count", sum(value(1))));

        MorphiaCursor<User> cursor = pipeline.execute(User.class);
        while (cursor.hasNext()) {
            cursor.next();
        }
    }

    @Test
    public void testDateToString() throws ParseException {
        Date joined = new SimpleDateFormat("yyyy-MM-dd z").parse("2016-05-01 UTC");
        getDs().save(new User("John Doe", joined));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .project(Projection.of()
                                                            .include("string", dateToString()
                                                                                   .format("%Y-%m-%d")
                                                                                   .date(field("joined"))));

        for (Iterator<StringDates> it = pipeline.execute(StringDates.class); it.hasNext(); ) {
            assertEquals("2016-05-01", it.next().string);
        }
    }

    @Test
    public void testGenericAccumulatorUsage() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<CountResult> aggregation = getDs().aggregate(Book.class)
                                                   .group(of(
                                                       dev.morphia.aggregation.experimental.stages.Group.id("author"))
                                                              .field("count", sum(value(1))))
                                                   .sort(Sort.on()
                                                             .ascending("_id"))
                                                   .execute(CountResult.class);

        CountResult result1 = aggregation.next();
        CountResult result2 = aggregation.next();
        Assert.assertFalse("Expecting two results", aggregation.hasNext());
        Assert.assertEquals("Dante", result1.getAuthor());
        Assert.assertEquals(3, result1.getCount());
        Assert.assertEquals("Homer", result2.getAuthor());
        Assert.assertEquals(2, result2.getCount());
    }

    @Test
    public void testGeoNearWithGeoJson() {
        // given
        Point londonPoint = new Point(new Position(51.5286416, -0.1015987));
        City london = new City("London", londonPoint);
        getDs().save(london);
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<City> citiesOrderedByDistanceFromLondon = getDs().aggregate(City.class)
                                                                  .geoNear(
                                                                      to(londonPoint)
                                                                          .distanceField("distance")
                                                                          .spherical(true))
                                                                  .execute(City.class);

        // then
        Assert.assertTrue(citiesOrderedByDistanceFromLondon.hasNext());
        Assert.assertEquals(london, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(manchester, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(sevilla, citiesOrderedByDistanceFromLondon.next());
        Assert.assertFalse(citiesOrderedByDistanceFromLondon.hasNext());
    }

    @Test
    public void testGeoNearWithLegacyCoords() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        PlaceWithLegacyCoords london = new PlaceWithLegacyCoords(new double[]{longitude, latitude}, "London");
        getDs().save(london);
        PlaceWithLegacyCoords manchester = new PlaceWithLegacyCoords(new double[]{-2.2235922, 53.4722454}, "Manchester");
        getDs().save(manchester);
        PlaceWithLegacyCoords sevilla = new PlaceWithLegacyCoords(new double[]{-5.9550582, 37.3753708}, "Sevilla");
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<PlaceWithLegacyCoords> cities = getDs().aggregate(PlaceWithLegacyCoords.class)
                                                        .geoNear(to(new double[]{latitude, longitude})
                                                                     .distanceField("distance")
                                                                     .spherical(false))
                                                        .execute(PlaceWithLegacyCoords.class);

        // then
        Assert.assertTrue(cities.hasNext());
        Assert.assertEquals(sevilla, cities.next());
        Assert.assertEquals(london, cities.next());
        Assert.assertEquals(manchester, cities.next());
        Assert.assertFalse(cities.hasNext());
    }

    @Test
    public void testGeoNearWithSphericalGeometry() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        City london = new City("London", new Point(new Position(latitude, longitude)));
        getDs().save(london);
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<City> cities = getDs().aggregate(City.class)
                                       .geoNear(to(new double[]{latitude, longitude})
                                                    .distanceField("distance")
                                                    .spherical(true))
                                       .execute(City.class);

        // then
        Assert.assertTrue(cities.hasNext());
        Assert.assertEquals(london, cities.next());
        Assert.assertEquals(manchester, cities.next());
        Assert.assertEquals(sevilla, cities.next());
        Assert.assertFalse(cities.hasNext());
    }

    @Test
    public void testLimit() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<Book> aggregate = getDs().aggregate(Book.class)
                                          .limit(2)
                                          .execute(Book.class);
        int count = 0;
        while (aggregate.hasNext()) {
            aggregate.next();
            count++;
        }
        Assert.assertEquals(2, count);
    }

    /**
     * Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
     */
    @Test
    @Ignore("custom collection name support needed")
    public void testLookup() {
        getDs().save(asList(new Order(1, "abc", 12, 2),
            new Order(2, "jkl", 20, 1),
            new Order(3)));
        List<Inventory> inventories = asList(new Inventory(1, "abc", "product 1", 120),
            new Inventory(2, "def", "product 2", 80),
            new Inventory(3, "ijk", "product 3", 60),
            new Inventory(4, "jkl", "product 4", 70),
            new Inventory(5, null, "Incomplete"),
            new Inventory(6));
        getDs().save(inventories);

        getDs().aggregate(Order.class)
               .lookup(Lookup.from("inventory")
                             .localField("item")
                             .foreignField("sku")
                             .as("inventoryDocs"))
               .out(Out.to(Lookedup.class))
               .execute();
        List<Order> lookups = getAds().createQuery("lookups", Order.class)
                                      .execute(new FindOptions()
                                                   .sort(ascending("_id")))
                                      .toList();
        Assert.assertEquals(inventories.get(0), lookups.get(0).inventoryDocs.get(0));
        Assert.assertEquals(inventories.get(3), lookups.get(1).inventoryDocs.get(0));
        Assert.assertEquals(inventories.get(4), lookups.get(2).inventoryDocs.get(0));
        Assert.assertEquals(inventories.get(5), lookups.get(2).inventoryDocs.get(1));
    }

    @Test
    public void testNullGroupId() {
        getDs()
            .aggregate(User.class)
            .group(of()
                       .field("count", sum(value(1))))
            .execute(User.class);

    }

    @Test
    public void testOut() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        dev.morphia.aggregation.experimental.AggregationOptions options = new dev.morphia.aggregation.experimental.AggregationOptions();
        Iterator<Author> aggregate = getDs().aggregate(Book.class)
                                            .group(of(id("author"))
                                                       .field("books", push(field("title"))))
                                            .out(Out.to(Author.class))
                                            .execute(Author.class, options);

        Assert.assertEquals(2, getMapper().getCollection(Author.class).countDocuments());
        Author author;
        do {
            author = aggregate.next();
        } while (!author.name.equals("Homer"));
        Assert.assertEquals("Homer", author.name);
        Assert.assertEquals(asList("The Odyssey", "Iliad"), author.books);

        getDs().aggregate(Book.class)
               .group(of(id("author"))
                          .field("books", push(field("title"))))
               .out(Out.to("different"))
               .execute();

        Assert.assertEquals(2, getDatabase().getCollection("different").countDocuments());
    }

    @Test
    public void testOutNamedCollection() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
            new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
            new Book("Eclogues", "Dante", 2, "Italian", ""),
            new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
            new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel")));

        getDs().aggregate(Book.class)
               .match(getDs().getQueryFactory().createQuery(getDs())
                             .filter(eq("author", "Homer")))
               .group(of(id("author"))
                          .field("copies", sum(field("copies"))))
               .out(Out.to("testAverage"))
               .execute();
        try (MongoCursor<Document> testAverage = getDatabase().getCollection("testAverage").find().iterator()) {
            Assert.assertEquals(20, testAverage.next().get("copies"));
        }
    }

    @Test
    public void testSkip() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Book book = getDs().aggregate(Book.class)
                           .skip(2)
                           .execute(Book.class)
                           .next();
        Assert.assertEquals("Eclogues", book.title);
        Assert.assertEquals("Dante", book.author);
        Assert.assertEquals(2, book.copies.intValue());
    }

    @Test
    public void testSortByCount() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));


        Iterator<SortByCountResult> aggregate = getDs().aggregate(Book.class)
                                                       .sortByCount(field("author"))
                                                       .execute(SortByCountResult.class);
        SortByCountResult result1 = aggregate.next();
        assertEquals(result1.id, "Dante");
        assertEquals(result1.count, 3);

        SortByCountResult result2 = aggregate.next();
        assertEquals(result2.id, "Homer");
        assertEquals(result2.count, 2);

    }

    @Test
    public void testUnwind() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        getDs().save(asList(new User("jane", format.parse("2011-03-02"), "golf", "racquetball"),
            new User("joe", format.parse("2012-07-02"), "tennis", "golf", "swimming"),
            new User("john", format.parse("2012-07-02"))));

        Iterator<User> aggregate = getDs().aggregate(User.class)
                                          .project(Projection.of()
                                                             .include("name")
                                                             .include("joined")
                                                             .include("likes"))
                                          .unwind(Unwind.on("likes"))
                                          .execute(User.class);
        int count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assert.assertEquals("jane", user.name);
                    Assert.assertEquals("golf", user.likes.get(0));
                    break;
                case 1:
                    Assert.assertEquals("jane", user.name);
                    Assert.assertEquals("racquetball", user.likes.get(0));
                    break;
                case 2:
                    Assert.assertEquals("joe", user.name);
                    Assert.assertEquals("tennis", user.likes.get(0));
                    break;
                case 3:
                    Assert.assertEquals("joe", user.name);
                    Assert.assertEquals("golf", user.likes.get(0));
                    break;
                case 4:
                    Assert.assertEquals("joe", user.name);
                    Assert.assertEquals("swimming", user.likes.get(0));
                    break;
                default:
                    fail("Should only find 5 elements");
            }
            count++;
        }

        aggregate = getDs().aggregate(User.class)
                           .project(Projection.of()
                                              .include("name")
                                              .include("joined")
                                              .include("likes"))
                           .unwind(Unwind.on("likes")
                                         .preserveNullAndEmptyArrays(true))
                           .execute(User.class);
        count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assert.assertEquals("jane", user.name);
                    Assert.assertEquals("golf", user.likes.get(0));
                    break;
                case 1:
                    Assert.assertEquals("jane", user.name);
                    Assert.assertEquals("racquetball", user.likes.get(0));
                    break;
                case 2:
                    Assert.assertEquals("joe", user.name);
                    Assert.assertEquals("tennis", user.likes.get(0));
                    break;
                case 3:
                    Assert.assertEquals("joe", user.name);
                    Assert.assertEquals("golf", user.likes.get(0));
                    break;
                case 4:
                    Assert.assertEquals("joe", user.name);
                    Assert.assertEquals("swimming", user.likes.get(0));
                    break;
                case 5:
                    Assert.assertEquals("john", user.name);
                    Assert.assertNull(user.likes);
                    break;
                default:
                    fail("Should only find 6 elements");
            }
            count++;
        }
    }

    @Test
    public void testUserPreferencesPipeline() {
        final MorphiaCursor<City> pipeline = getDs().aggregate(City.class)  /* the class is irrelevant for this test */
                                                    .group(of(
                                                        id("state"))
                                                               .field("total_pop", sum(field("pop"))))
                                                    .match(getDs().find(Book.class)
                                                                  .disableValidation()
                                                                  .filter(gte("total_pop", 10000000)))
                                                    .execute(City.class);
        while (pipeline.hasNext()) {
            pipeline.next();
        }
    }

    private Document getDocument(final Document document, final String... path) {
        Document current = document;
        for (String step : path) {
            Object next = current.get(step);
            Assert.assertNotNull(format("Could not find %s in \n%s", step, current), next);
            current = (Document) next;
        }
        return current;
    }

    @Entity("authors")
    public static class Author {
        @Id
        private String name;
        private List<String> books;
    }

    @Entity(value = "books", useDiscriminator = false)
    public static final class Book {
        @Id
        private ObjectId id;
        private String title;
        private String author;
        private Integer copies;
        private List<String> tags;

        private Book() {
        }

        public Book(final String title, final String author, final Integer copies, final String... tags) {
            this.title = title;
            this.author = author;
            this.copies = copies;
            this.tags = asList(tags);
        }

        @Override
        public String toString() {
            return format("Book{title='%s', author='%s', copies=%d, tags=%s}", title, author, copies, tags);
        }
    }

    public static class BooksBucketResult extends BucketAutoResult {
        private Set<String> authors;

        public Set<String> getAuthors() {
            return authors;
        }

        public void setAuthors(final Set<String> authors) {
            this.authors = authors;
        }
    }

    @Entity
    public static class BucketAutoResult {

        @Id
        private MinMax id;
        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public MinMax getId() {
            return id;
        }

        public void setId(final MinMax id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "BucketAutoResult{"
                   + "id=" + id
                   + ", count=" + count
                   + '}';
        }

        @Embedded
        public static class MinMax {
            private int min;
            private int max;

            public int getMax() {
                return max;
            }

            public void setMax(final int max) {
                this.max = max;
            }

            public int getMin() {
                return min;
            }

            public void setMin(final int min) {
                this.min = min;
            }

            @Override
            public String toString() {
                return "MinMax{"
                       + "min=" + min
                       + ", max=" + max
                       + '}';
            }
        }
    }

    @Entity
    public static class BucketResult {
        @Id
        private String id;
        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "BucketResult{"
                   + "id="
                   + id
                   + ", count=" + count
                   + '}';
        }
    }

    @Entity("counts")
    public static class CountResult {

        @Id
        private String author;
        @AlsoLoad("value")
        private int count;

        public String getAuthor() {
            return author;
        }

        public int getCount() {
            return count;
        }
    }

    @Entity(value = "inventory", useDiscriminator = false)
    public static class Inventory {
        @Id
        private int id;
        private String sku;
        private String description;
        private int instock;

        public Inventory() {
        }

        Inventory(final int id) {
            this.id = id;
        }

        Inventory(final int id, final String sku, final String description) {
            this.id = id;
            this.sku = sku;
            this.description = description;
        }

        Inventory(final int id, final String sku, final String description, final int instock) {
            this.id = id;
            this.sku = sku;
            this.description = description;
            this.instock = instock;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public int getInstock() {
            return instock;
        }

        public void setInstock(final int instock) {
            this.instock = instock;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(final String sku) {
            this.sku = sku;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (sku != null ? sku.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + instock;
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Inventory)) {
                return false;
            }

            final Inventory inventory = (Inventory) o;

            if (id != inventory.id) {
                return false;
            }
            if (instock != inventory.instock) {
                return false;
            }
            if (sku != null ? !sku.equals(inventory.sku) : inventory.sku != null) {
                return false;
            }
            return description != null ? description.equals(inventory.description) : inventory.description == null;

        }
    }

    @Entity("lookups")
    private static class Lookedup {
        List<Inventory> inventoryDocs;
        @Id
        private int id;
        private String item;
        private int price;
        private int quantity;
    }

    @Entity("orders")
    private static class Order {
        @Id
        private int id;
        private String item;
        private int price;
        private int quantity;
        private List<Inventory> inventoryDocs;

        private Order() {
        }

        Order(final int id) {
            this.id = id;
        }

        Order(final int id, final String item, final int price, final int quantity) {
            this.id = id;
            this.item = item;
            this.price = price;
            this.quantity = quantity;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getItem() {
            return item;
        }

        public void setItem(final String item) {
            this.item = item;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(final int price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(final int quantity) {
            this.quantity = quantity;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (item != null ? item.hashCode() : 0);
            result = 31 * result + price;
            result = 31 * result + quantity;
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Order)) {
                return false;
            }

            final Order order = (Order) o;

            if (id != order.id) {
                return false;
            }
            if (price != order.price) {
                return false;
            }
            if (quantity != order.quantity) {
                return false;
            }
            return item != null ? item.equals(order.item) : order.item == null;

        }

    }

    @Entity
    public static class SortByCountResult {
        @Id
        private String id;
        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "SortByCountResult{"
                   + "id=" + id
                   + ", count=" + count
                   + '}';
        }
    }

    @Entity
    private static class StringDates {
        @Id
        private ObjectId id;
        private String string;
    }
}
