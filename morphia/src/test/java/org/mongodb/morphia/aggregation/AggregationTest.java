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

package org.mongodb.morphia.aggregation;

import com.mongodb.AggregationOptions;
import com.mongodb.AggregationOptions.OutputMode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Validation;
import org.mongodb.morphia.geo.City;
import org.mongodb.morphia.geo.PlaceWithLegacyCoords;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.query.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.mongodb.AggregationOptions.builder;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.addToSet;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;
import static org.mongodb.morphia.aggregation.Group.push;
import static org.mongodb.morphia.aggregation.Group.sum;
import static org.mongodb.morphia.aggregation.Projection.divide;
import static org.mongodb.morphia.aggregation.Projection.expression;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.geo.GeoJson.point;
import static org.mongodb.morphia.query.Sort.ascending;

public class AggregationTest extends TestBase {

    @Test
    public void testCollation() {
        checkMinServerVersion(3.4);
        getDs().save(asList(new User("john doe", new Date()), new User("John Doe", new Date())));

        Query query = getDs().find(User.class).field("name").equal("john doe");
        AggregationPipeline pipeline = getDs()
            .createAggregation(User.class)
            .match(query);
        Assert.assertEquals(1, count(pipeline.aggregate(User.class)));

        Assert.assertEquals(2, count(pipeline.aggregate(User.class,
                                                        builder()
                                                            .collation(Collation.builder()
                                                                                .locale("en")
                                                                                .collationStrength(
                                                                                    CollationStrength.SECONDARY)
                                                                                .build()).build())));
    }

    @Test
    public void testBypassDocumentValidation() {
        checkMinServerVersion(3.2);
        getDs().save(asList(new User("john doe", new Date()), new User("John Doe", new Date())));

        MongoDatabase database = getMongoClient().getDatabase(TEST_DB_NAME);
        database.getCollection("out_users").drop();
        database.createCollection("out_users", new CreateCollectionOptions()
            .validationOptions(new ValidationOptions()
                                   .validator(Document.parse("{ age : { gte : 13 } }"))));

        try {
            getDs()
                .createAggregation(User.class)
                .match(getDs().find(User.class).field("name").equal("john doe"))
                .out("out_users", User.class);
            fail("Document validation should have complained.");
        } catch (MongoCommandException e) {
            // expected
        }

        getDs()
            .createAggregation(User.class)
            .match(getDs().find(User.class).field("name").equal("john doe"))
            .out("out_users", User.class, builder()
                .bypassDocumentValidation(true)
                .build());

        Assert.assertEquals(1, getAds().find("out_users", User.class).count());
    }

    @Test
    public void testDateAggregation() {
        AggregationPipeline pipeline = getDs()
            .createAggregation(User.class)
            .group(id(grouping("month", accumulator("$month", "date")),
                         grouping("year", accumulator("$year", "date"))),
                grouping("count", accumulator("$sum", 1)));
        final DBObject group = ((AggregationPipelineImpl) pipeline).getStages().get(0);
        final DBObject id = getDBObject(group, "$group", "_id");
        Assert.assertEquals(new BasicDBObject("$month", "$date"), id.get("month"));
        Assert.assertEquals(new BasicDBObject("$year", "$date"), id.get("year"));

        pipeline.aggregate(User.class);
    }

    @Test
    public void testNullGroupId() {
        AggregationPipeline pipeline = getDs()
            .createAggregation(User.class)
            .group(grouping("count", accumulator("$sum", 1)));

        final DBObject group = ((AggregationPipelineImpl) pipeline).getStages().get(0);
        Assert.assertNull(group.get("_id"));

        pipeline.aggregate(User.class);
    }

    @Test
    public void testDateToString() throws ParseException {
        checkMinServerVersion(3.0);
        Date joined = new SimpleDateFormat("yyyy-MM-dd z").parse("2016-05-01 UTC");
        getDs().save(new User("John Doe", joined));
        AggregationPipeline pipeline = getDs()
            .createAggregation(User.class)
            .project(projection("string", expression("$dateToString",
                                                     new BasicDBObject("format", "%Y-%m-%d")
                                                         .append("date", "$joined"))));

        Iterator<StringDates> aggregate = pipeline.aggregate(StringDates.class,
                                                             builder()
            .outputMode(OutputMode.CURSOR)
            .build());
        while (aggregate.hasNext()) {
            StringDates next = aggregate.next();
            Assert.assertEquals("2016-05-01", next.string);
        }
    }

    @Test
    public void testGenericAccumulatorUsage() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        Iterator<CountResult> aggregation = getDs().createAggregation(Book.class)
                                                   .group("author", grouping("count", accumulator("$sum", 1)))
                                                   .sort(ascending("_id"))
                                                   .aggregate(CountResult.class);

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
        Point londonPoint = point(51.5286416, -0.1015987);
        City london = new City("London", londonPoint);
        getDs().save(london);
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        getDs().save(manchester);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<City> citiesOrderedByDistanceFromLondon = getDs().createAggregation(City.class)
                                                                  .geoNear(GeoNear.builder("distance")
                                                                                  .setNear(londonPoint)
                                                                                  .setSpherical(true)
                                                                                  .build())
                                                                  .aggregate(City.class);

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
        Iterator<PlaceWithLegacyCoords> citiesOrderedByDistanceFromLondon = getDs()
            .createAggregation(PlaceWithLegacyCoords.class)
            .geoNear(GeoNear.builder("distance")
                            .setNear(latitude, longitude)
                            .setSpherical(false)
                            .build())
            .aggregate(PlaceWithLegacyCoords.class);

        // then
        Assert.assertTrue(citiesOrderedByDistanceFromLondon.hasNext());
        Assert.assertEquals(london, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(manchester, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(sevilla, citiesOrderedByDistanceFromLondon.next());
        Assert.assertFalse(citiesOrderedByDistanceFromLondon.hasNext());
    }

    @Test
    public void testGeoNearWithSphericalGeometry() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        City london = new City("London", point(latitude, longitude));
        getDs().save(london);
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        getDs().save(manchester);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<City> citiesOrderedByDistanceFromLondon = getDs().createAggregation(City.class)
                                                                  .geoNear(GeoNear.builder("distance")
                                                                                  .setNear(latitude, longitude)
                                                                                  .setSpherical(true)
                                                                                  .build())
                                                                  .aggregate(City.class);

        // then
        Assert.assertTrue(citiesOrderedByDistanceFromLondon.hasNext());
        Assert.assertEquals(london, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(manchester, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(sevilla, citiesOrderedByDistanceFromLondon.next());
        Assert.assertFalse(citiesOrderedByDistanceFromLondon.hasNext());
    }

    @Test
    public void testLimit() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        Iterator<Book> aggregate = getDs().createAggregation(Book.class)
                                          .limit(2)
                                          .aggregate(Book.class);
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
    public void testLookup() {
        checkMinServerVersion(3.2);
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

        getDs().createAggregation(Order.class)
               .lookup("inventory", "item", "sku", "inventoryDocs")
               .out("lookups", Order.class);
        List<Order> lookups = getAds().createQuery("lookups", Order.class)
                                      .order("_id")
                                      .asList();
        Assert.assertEquals(inventories.get(0), lookups.get(0).inventoryDocs.get(0));
        Assert.assertEquals(inventories.get(3), lookups.get(1).inventoryDocs.get(0));
        Assert.assertEquals(inventories.get(4), lookups.get(2).inventoryDocs.get(0));
        Assert.assertEquals(inventories.get(5), lookups.get(2).inventoryDocs.get(1));
    }

    @Test
    public void testOut() {
        checkMinServerVersion(2.6);
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        AggregationOptions options = builder()
            .outputMode(AggregationOptions.OutputMode.CURSOR)
            .build();
        Iterator<Author> aggregate = getDs().createAggregation(Book.class)
                                            .group("author", grouping("books", push("title")))
                                            .out(Author.class, options);
        Assert.assertEquals(2, getDs().getCollection(Author.class).count());
        Author author = aggregate.next();
        Assert.assertEquals("Homer", author.name);
        Assert.assertEquals(asList("The Odyssey", "Iliad"), author.books);

        getDs().createAggregation(Book.class)
               .group("author", grouping("books", push("title")))
               .out("different", Author.class);

        Assert.assertEquals(2, getDb().getCollection("different").count());
    }

    @Test
    public void testOutNamedCollection() {
        checkMinServerVersion(2.6);
        getDs().save(asList(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
                            new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
                            new Book("Eclogues", "Dante", 2, "Italian", ""),
                            new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
                            new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel")));

        getDs().createAggregation(Book.class)
               .match(getDs().getQueryFactory().createQuery(getDs())
                             .field("author").equal("Homer"))
               .group("author", grouping("copies", sum("copies")))
               .out("testAverage", Author.class);
        DBCursor testAverage = getDb().getCollection("testAverage").find();
        Assert.assertNotNull(testAverage);
        try {
            Assert.assertEquals(20, testAverage.next().get("copies"));
        } finally {
            testAverage.close();
        }
    }

    @Test
    public void testProjection() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        final AggregationPipeline pipeline = getDs().createAggregation(Book.class)
                                                    .group("author", grouping("copies", sum("copies")))
                                                    .project(projection("_id").suppress(),
                                                             projection("author", "_id"),
                                                             projection("copies", divide(projection("copies"), 5)))
                                                    .sort(ascending("author"));
        Iterator<Book> aggregate = pipeline.aggregate(Book.class);
        Book book = aggregate.next();
        Assert.assertEquals("Dante", book.author);
        Assert.assertEquals(1, book.copies.intValue());

        final List<DBObject> stages = ((AggregationPipelineImpl) pipeline).getStages();
        Assert.assertEquals(stages.get(0), obj("$group", obj("_id", "$author").append("copies", obj("$sum", "$copies"))));
        Assert.assertEquals(stages.get(1), obj("$project", obj("_id", 0)
            .append("author", "$_id")
            .append("copies", obj("$divide", Arrays.<Object>asList("$copies", 5)))));

    }

    @Test
    public void testSkip() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                            new Book("Divine Comedy", "Dante", 1),
                            new Book("Eclogues", "Dante", 2),
                            new Book("The Odyssey", "Homer", 10),
                            new Book("Iliad", "Homer", 10)));

        Book book = getDs().createAggregation(Book.class)
                           .skip(2)
                           .aggregate(Book.class).next();
        Assert.assertEquals("Eclogues", book.title);
        Assert.assertEquals("Dante", book.author);
        Assert.assertEquals(2, book.copies.intValue());
    }

    @Test
    public void testUnwind() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        getDs().save(asList(new User("jane", format.parse("2011-03-02"), "golf", "racquetball"),
                            new User("joe", format.parse("2012-07-02"), "tennis", "golf", "swimming")));

        Iterator<User> aggregate = getDs().createAggregation(User.class)
                                          .project(projection("_id").suppress(), projection("name"), projection("joined"),
                                                   projection("likes"))
                                          .unwind("likes")
                                          .aggregate(User.class);
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
    }

    @Test
    public void testUserPreferencesPipeline() {
        final AggregationPipeline pipeline = getDs().createAggregation(Book.class)  /* the class is irrelevant for this test */
                                                    .group("state", Group.grouping("total_pop", sum("pop")))
                                                    .match(getDs().find(Book.class)
                                                                  .disableValidation()
                                                                  .field("total_pop").greaterThanOrEq(10000000));
        DBObject group = obj("$group", obj("_id", "$state")
            .append("total_pop", obj("$sum", "$pop")));

        DBObject match = obj("$match", obj("total_pop", obj("$gte", 10000000)));

        final List<DBObject> stages = ((AggregationPipelineImpl) pipeline).getStages();
        Assert.assertEquals(stages.get(0), group);
        Assert.assertEquals(stages.get(1), match);
    }

    @Test
    public void testGroupWithProjection() {
        AggregationPipeline pipeline =
            getDs().createAggregation(Author.class)
                   .group("subjectHash",
                           grouping("authors", addToSet("fromAddress.address")),
                           grouping("messageDataSet", grouping("$addToSet",
                                   projection("sentDate", "sentDate"),
                                   projection("messageId", "_id"))),
                           grouping("messageCount", accumulator("$sum", 1)))
                   .limit(10)
                   .skip(0);
        List<DBObject> stages = ((AggregationPipelineImpl) pipeline).getStages();
        DBObject group = stages.get(0);
        DBObject addToSet = getDBObject(group, "$group", "messageDataSet", "$addToSet");
        Assert.assertNotNull(addToSet);
        Assert.assertEquals(addToSet.get("sentDate"), "$sentDate");
        Assert.assertEquals(addToSet.get("messageId"), "$_id");
    }

    @Test
    public void testAdd() {
        AggregationPipeline pipeline = getDs()
            .createAggregation(Book.class)
            .group(grouping("summation",
                            accumulator("$sum", accumulator("$add", asList("$amountFromTBInDouble", "$amountFromParentPNLInDouble"))
                            )));

        DBObject group = (DBObject) ((AggregationPipelineImpl) pipeline).getStages().get(0).get("$group");
        DBObject summation = (DBObject) group.get("summation");
        DBObject sum = (DBObject) summation.get("$sum");
        List<?> add = (List<?>) sum.get("$add");
        Assert.assertTrue(add.get(0) instanceof String);
        Assert.assertEquals("$amountFromTBInDouble", add.get(0));
        pipeline.aggregate(User.class);
    }

    private DBObject getDBObject(final DBObject dbObject, final String... path) {
        DBObject current = dbObject;
        for (String step : path) {
            Object next = current.get(step);
            Assert.assertNotNull(format("Could not find %s in \n%s", step, current), next);
            current = (DBObject) next;
        }
        return current;
    }

    static class StringDates {
        @Id
        private ObjectId id;
        private String string;
    }

    @Entity(value = "books", noClassnameStored = true)
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

    @Entity("authors")
    public static class Author {
        @Id
        private String name;
        private List<String> books;
    }

    @Entity("users")
    @Validation("{ age : { $gte : 13 } }")
    private static final class User {
        @Id
        private ObjectId id;
        private String name;
        private Date joined;
        private List<String> likes;
        private int age;

        private User() {
        }

        private User(final String name, final Date joined, final String... likes) {
            this.name = name;
            this.joined = joined;
            this.likes = asList(likes);
        }

        @Override
        public String toString() {
            return format("User{name='%s', joined=%s, likes=%s}", name, joined, likes);
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

    @Entity("orders")
    private static class Order {
        @Id
        private int id;
        private String item;
        private int price;
        private int quantity;
        @Embedded()
        private List<Inventory> inventoryDocs;

        public Order() {
        }

        public Order(final int id) {
            this.id = id;
        }

        public Order(final int id, final String item, final int price, final int quantity) {
            this.id = id;
            this.item = item;
            this.price = price;
            this.quantity = quantity;
        }

        public List<Inventory> getInventoryDocs() {
            return inventoryDocs;
        }

        public void setInventoryDocs(final List<Inventory> inventoryDocs) {
            this.inventoryDocs = inventoryDocs;
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

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (item != null ? item.hashCode() : 0);
            result = 31 * result + price;
            result = 31 * result + quantity;
            return result;
        }        @Override
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

    @Entity(value = "inventory", noClassnameStored = true)
    public static class Inventory {
        @Id
        private int id;
        private String sku;
        private String description;
        private int instock;

        public Inventory() {
        }

        public Inventory(final int id) {
            this.id = id;
        }

        public Inventory(final int id, final String sku, final String description) {
            this.id = id;
            this.sku = sku;
            this.description = description;
        }

        public Inventory(final int id, final String sku, final String description, final int instock) {
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

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
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

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (sku != null ? sku.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + instock;
            return result;
        }
    }
}
