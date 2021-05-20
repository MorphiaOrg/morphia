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

package dev.morphia.test.aggregation;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BucketGranularity;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.AggregationOptions;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.User;
import dev.morphia.test.models.geo.GeoCity;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.month;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.year;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.stages.AutoBucket.autoBucket;
import static dev.morphia.aggregation.experimental.stages.Bucket.bucket;
import static dev.morphia.aggregation.experimental.stages.GeoNear.geoNear;
import static dev.morphia.aggregation.experimental.stages.Group.group;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Lookup.lookup;
import static dev.morphia.aggregation.experimental.stages.Out.to;
import static dev.morphia.aggregation.experimental.stages.Projection.project;
import static dev.morphia.aggregation.experimental.stages.Sort.sort;
import static dev.morphia.aggregation.experimental.stages.Unwind.unwind;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.List.of;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
public class AggregationTest extends TestBase {

    @Test
    public void testBasicGrouping() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        MorphiaCursor<Author> aggregate = getDs().aggregate(Book.class)
                                                 .group(group(id("author"))
                                                            .field("books", push(field("title"))))
                                                 .sort(sort().ascending("name"))
                                                 .execute(Author.class);

        Map<Object, List<Author>> authors = aggregate.toList()
                                                     .stream()
                                                     .collect(Collectors.groupingBy(a -> a.name));
        Assert.assertEquals(authors.size(), 2, "Expecting two results");
        Assert.assertEquals(authors.get("Dante").get(0).books, of("The Banquet", "Divine Comedy", "Eclogues"), authors.toString());
        Assert.assertEquals(authors.get("Homer").get(0).books, of("The Odyssey", "Iliad"), authors.toString());
    }

    @Test
    public void testBucketAutoWithGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5),
            new Book("Divine Comedy", "Dante", 7),
            new Book("Eclogues", "Dante", 40),
            new Book("The Odyssey", "Homer", 21)));

        Iterator<BooksBucketResult> aggregate = getDs().aggregate(Book.class)
                                                       .autoBucket(autoBucket()
                                                                       .groupBy(field("copies"))
                                                                       .buckets(3)
                                                                       .granularity(BucketGranularity.POWERSOF2)
                                                                       .outputField("authors", addToSet(field("author")))
                                                                       .outputField("count", sum(value(1))))
                                                       .execute(BooksBucketResult.class);
        BooksBucketResult result1 = aggregate.next();
        Assert.assertEquals(result1.getId().min, 4);
        Assert.assertEquals(result1.getId().max, 8);
        Assert.assertEquals(result1.getCount(), 2);
        Assert.assertEquals(result1.authors, singleton("Dante"));

        result1 = aggregate.next();
        Assert.assertEquals(result1.getId().min, 8);
        Assert.assertEquals(result1.getId().max, 32);
        Assert.assertEquals(result1.getCount(), 1);
        Assert.assertEquals(result1.authors, singleton("Homer"));

        result1 = aggregate.next();
        Assert.assertEquals(result1.getId().min, 32);
        Assert.assertEquals(result1.getId().max, 64);
        Assert.assertEquals(result1.getCount(), 1);
        Assert.assertEquals(result1.authors, singleton("Dante"));
        Assert.assertFalse(aggregate.hasNext());

    }

    @Test
    public void testBucketAutoWithoutGranularity() {
        getDs().save(asList(
            new Book("The Banquet", "Dante", 5),
            new Book("Divine Comedy", "Dante", 10),
            new Book("Eclogues", "Dante", 40),
            new Book("The Odyssey", "Homer", 21)));

        Iterator<BucketAutoResult> aggregate = getDs().aggregate(Book.class)
                                                      .autoBucket(autoBucket()
                                                                      .groupBy(field("copies"))
                                                                      .buckets(2))
                                                      .execute(BucketAutoResult.class);
        BucketAutoResult result1 = aggregate.next();
        Assert.assertEquals(result1.id.min, 5);
        Assert.assertEquals(result1.id.max, 21);
        Assert.assertEquals(result1.count, 2);
        result1 = aggregate.next();
        Assert.assertEquals(result1.id.min, 21);
        Assert.assertEquals(result1.id.max, 40);
        Assert.assertEquals(result1.count, 2);
        Assert.assertFalse(aggregate.hasNext());

    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBucketWithBoundariesWithSizeLessThanTwo() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        getDs().aggregate(Book.class)
               .bucket(bucket()
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
                                                  .bucket(bucket()
                                                              .groupBy(field("copies"))
                                                              .boundaries(value(1), value(5), value(10))
                                                              .defaultValue(-1)
                                                              .outputField("count", sum(value(1))))
                                                  .execute(BucketResult.class);

        BucketResult result2 = aggregate.next();
        Assert.assertEquals(result2.id, Integer.valueOf(-1));
        Assert.assertEquals(result2.count, 2);

        BucketResult result1 = aggregate.next();
        Assert.assertEquals(result1.id, Integer.valueOf(1));
        Assert.assertEquals(result1.count, 3);

    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBucketWithUnsortedBoundaries() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class)
                                                  .bucket(bucket()
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
                                                  .bucket(bucket()
                                                              .groupBy(field("copies"))
                                                              .boundaries(value(1), value(5), value(12)))
                                                  .execute(BucketResult.class);
        BucketResult result1 = aggregate.next();
        Assert.assertEquals(result1.id, Integer.valueOf(1));
        Assert.assertEquals(result1.count, 3);

        BucketResult result2 = aggregate.next();
        Assert.assertEquals(result2.id, Integer.valueOf(5));
        Assert.assertEquals(result2.count, 2);
    }

    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", LocalDate.now()), new User("John Doe", LocalDate.now())));

        Aggregation<User> pipeline = getDs().aggregate(User.class)
                                            .match(eq("name", "john doe"));
        Assert.assertEquals(count(pipeline.execute(User.class)), 1);

        Assert.assertEquals(count(pipeline.execute(User.class,
            new AggregationOptions().collation(Collation.builder()
                                                        .locale("en")
                                                        .collationStrength(SECONDARY)
                                                        .build()))), 2);
    }

    @Test
    public void testDateAggregation() {
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .group(group(
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
    public void testDateToString() {
        LocalDate joined = LocalDate.parse("2016-05-01 UTC", DateTimeFormatter.ofPattern("yyyy-MM-dd z"));
        getDs().save(new User("John Doe", joined));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .project(project()
                                                      .include("string", dateToString()
                                                                             .format("%Y-%m-%d")
                                                                             .date(field("joined"))));

        for (Iterator<StringDates> it = pipeline.execute(StringDates.class); it.hasNext(); ) {
            Assert.assertEquals(it.next().string, "2016-05-01");
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
                                                   .group(group(id("author"))
                                                              .field("count", sum(value(1))))
                                                   .sort(sort()
                                                             .ascending("_id"))
                                                   .execute(CountResult.class);

        CountResult result1 = aggregation.next();
        CountResult result2 = aggregation.next();
        Assert.assertFalse(aggregation.hasNext(), "Expecting two results");
        Assert.assertEquals(result1.getAuthor(), "Dante");
        Assert.assertEquals(result1.getCount(), 3);
        Assert.assertEquals(result2.getAuthor(), "Homer");
        Assert.assertEquals(result2.getCount(), 2);
    }

    @Test
    public void testGeoNearWithGeoJson() {
        // given
        Point londonPoint = new Point(new Position(51.5286416, -0.1015987));
        GeoCity london = new GeoCity("London", londonPoint);
        getDs().save(london);
        GeoCity manchester = new GeoCity("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        GeoCity sevilla = new GeoCity("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<GeoCity> cities = getDs().aggregate(GeoCity.class)
                                          .geoNear(geoNear(londonPoint)
                                                       .distanceField("distance")
                                                       .spherical(true))
                                          .execute(GeoCity.class);

        // then
        Assert.assertTrue(cities.hasNext());
        Assert.assertEquals(london, cities.next());
        Assert.assertEquals(manchester, cities.next());
        Assert.assertEquals(sevilla, cities.next());
        Assert.assertFalse(cities.hasNext());
    }

    @Test
    public void testGeoNearWithSphericalGeometry() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        GeoCity london = new GeoCity("London", new Point(new Position(latitude, longitude)));
        getDs().save(london);
        GeoCity manchester = new GeoCity("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        GeoCity sevilla = new GeoCity("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<GeoCity> cities = getDs().aggregate(GeoCity.class)
                                          .geoNear(geoNear(new double[]{latitude, longitude})
                                                       .distanceField("distance")
                                                       .spherical(true))
                                          .execute(GeoCity.class);

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
        Assert.assertEquals(count, 2);
    }

    /**
     * Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
     */
    @Test
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
               .lookup(lookup("inventory")
                           .localField("item")
                           .foreignField("sku")
                           .as("inventoryDocs"))
               .out(to(Lookedup.class));
        List<Order> lookups = getDs().find("lookups", Order.class)
                                     .iterator(new FindOptions().sort(ascending("_id")))
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
            .group(group()
                       .field("count", sum(value(1))))
            .execute(User.class);

    }

    @Test
    public void testOutNamedCollection() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
            new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
            new Book("Eclogues", "Dante", 2, "Italian", ""),
            new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
            new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel")));

        getDs().aggregate(Book.class)
               .match(eq("author", "Homer"))
               .group(group(id("author"))
                          .field("copies", sum(field("copies"))))
               .out(to("testAverage"));
        try (MongoCursor<Document> testAverage = getDatabase().getCollection("testAverage").find().iterator()) {
            Assert.assertEquals(testAverage.next().get("copies"), 20);
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
        Assert.assertEquals(book.title, "Eclogues");
        Assert.assertEquals(book.author, "Dante");
        Assert.assertEquals(book.copies.intValue(), 2);
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
        Assert.assertEquals(result1.id, "Dante");
        Assert.assertEquals(result1.count, 3);

        SortByCountResult result2 = aggregate.next();
        Assert.assertEquals(result2.id, "Homer");
        Assert.assertEquals(result2.count, 2);

    }

    @Test
    public void testUnwind() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        getDs().save(asList(new User("jane", LocalDate.parse("2011-03-02", format), "golf", "racquetball"),
            new User("joe", LocalDate.parse("2012-07-02", format), "tennis", "golf", "swimming"),
            new User("john", LocalDate.parse("2012-07-02", format))));

        Iterator<User> aggregate = getDs().aggregate(User.class)
                                          .project(project()
                                                       .include("name")
                                                       .include("joined")
                                                       .include("likes"))
                                          .unwind(unwind("likes"))
                                          .execute(User.class);
        int count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assert.assertEquals(user.name, "jane");
                    Assert.assertEquals(user.likes.get(0), "golf");
                    break;
                case 1:
                    Assert.assertEquals(user.name, "jane");
                    Assert.assertEquals(user.likes.get(0), "racquetball");
                    break;
                case 2:
                    Assert.assertEquals(user.name, "joe");
                    Assert.assertEquals(user.likes.get(0), "tennis");
                    break;
                case 3:
                    Assert.assertEquals(user.name, "joe");
                    Assert.assertEquals(user.likes.get(0), "golf");
                    break;
                case 4:
                    Assert.assertEquals(user.name, "joe");
                    Assert.assertEquals(user.likes.get(0), "swimming");
                    break;
                default:
                    Assert.fail("Should only find 5 elements");
            }
            count++;
        }

        aggregate = getDs().aggregate(User.class)
                           .project(project()
                                        .include("name")
                                        .include("joined")
                                        .include("likes"))
                           .unwind(unwind("likes")
                                       .preserveNullAndEmptyArrays(true))
                           .execute(User.class);
        count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assert.assertEquals(user.name, "jane");
                    Assert.assertEquals(user.likes.get(0), "golf");
                    break;
                case 1:
                    Assert.assertEquals(user.name, "jane");
                    Assert.assertEquals(user.likes.get(0), "racquetball");
                    break;
                case 2:
                    Assert.assertEquals(user.name, "joe");
                    Assert.assertEquals(user.likes.get(0), "tennis");
                    break;
                case 3:
                    Assert.assertEquals(user.name, "joe");
                    Assert.assertEquals(user.likes.get(0), "golf");
                    break;
                case 4:
                    Assert.assertEquals(user.name, "joe");
                    Assert.assertEquals(user.likes.get(0), "swimming");
                    break;
                case 5:
                    Assert.assertEquals(user.name, "john");
                    Assert.assertNull(user.likes);
                    break;
                default:
                    Assert.fail("Should only find 6 elements");
            }
            count++;
        }
    }

    @Test
    public void testUserPreferencesPipeline() {
        final MorphiaCursor<GeoCity> pipeline = getDs().aggregate(GeoCity.class)  /* the class is irrelevant for this test */
                                                       .group(group(
                                                           id("state"))
                                                                  .field("total_pop", sum(field("pop"))))
                                                       .match(gte("total_pop", 10000000))
                                                       .execute(GeoCity.class);
        while (pipeline.hasNext()) {
            pipeline.next();
        }
    }

    private Document getDocument(Document document, String... path) {
        Document current = document;
        for (String step : path) {
            Object next = current.get(step);
            Assert.assertNotNull(next, format("Could not find %s in \n%s", step, current));
            current = (Document) next;
        }
        return current;
    }

    @Entity("authors")
    public static class Author {
        @Id
        private String name;
        private List<String> books;

        @Override
        public String toString() {
            return new StringJoiner(", ", Author.class.getSimpleName() + "[", "]")
                       .add("name='" + name + "'")
                       .add("books=" + books)
                       .toString();
        }
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

        public Book(String title, String author, Integer copies, String... tags) {
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

        public void setAuthors(Set<String> authors) {
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

        public void setCount(int count) {
            this.count = count;
        }

        public MinMax getId() {
            return id;
        }

        public void setId(MinMax id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "BucketAutoResult{"
                   + "id=" + id
                   + ", count=" + count
                   + '}';
        }

        @Entity
        public static class MinMax {
            private int min;
            private int max;

            public int getMax() {
                return max;
            }

            public void setMax(int max) {
                this.max = max;
            }

            public int getMin() {
                return min;
            }

            public void setMin(int min) {
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
        private Integer id;
        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
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

        Inventory(int id) {
            this.id = id;
        }

        Inventory(int id, String sku, String description) {
            this.id = id;
            this.sku = sku;
            this.description = description;
        }

        Inventory(int id, String sku, String description, int instock) {
            this.id = id;
            this.sku = sku;
            this.description = description;
            this.instock = instock;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getInstock() {
            return instock;
        }

        public void setInstock(int instock) {
            this.instock = instock;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
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
        public boolean equals(Object o) {
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

        Order(int id) {
            this.id = id;
        }

        Order(int id, String item, int price, int quantity) {
            this.id = id;
            this.item = item;
            this.price = price;
            this.quantity = quantity;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
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
        public boolean equals(Object o) {
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

        public void setCount(int count) {
            this.count = count;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
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
