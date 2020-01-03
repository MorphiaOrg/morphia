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

package dev.morphia.aggregation.experimental;

import com.mongodb.client.model.Collation;
import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.model.Author;
import dev.morphia.aggregation.experimental.model.Book;
import dev.morphia.aggregation.experimental.model.CountResult;
import dev.morphia.aggregation.experimental.model.Inventory;
import dev.morphia.aggregation.experimental.model.Order;
import dev.morphia.aggregation.experimental.model.Sales;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.experimental.Lookup.from;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.add;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.sum;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.push;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Projection.of;
import static dev.morphia.aggregation.experimental.stages.Sort.on;
import static java.util.Arrays.asList;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;

public class AggregationTest extends TestBase {

    @Test
    public void testAdd() {
        getMapper().map(Sales.class);
        List<Document> parse = asList(
            parse("{ \"_id\" : 1, \"item\" : \"abc\", \"price\" : 10, \"fee\" : 2, date: ISODate(\"2014-03-01T08:00:00Z\") }"),
            parse("{ \"_id\" : 2, \"item\" : \"jkl\", \"price\" : 20, \"fee\" : 1, date: ISODate(\"2014-03-01T09:00:00Z\") }"),
            parse("{ \"_id\" : 3, \"item\" : \"xyz\", \"price\" : 5,  \"fee\" : 0, date: ISODate(\"2014-03-15T09:00:00Z\") }"));
        getDatabase().getCollection("sales", Document.class)
                     .insertMany(parse);
        Aggregation<Sales> pipeline = getDs()
                                          .aggregate(Sales.class)
                                          .project(of()
                                                       .include("item")
                                                       .include("total", 
                                                           add(field("price"), field("fee"))));

        List<Document> list = pipeline.execute(Document.class).toList();
        List<Document> expected = asList(
            parse("{ \"_id\" : 1, \"item\" : \"abc\", \"total\" : 12 }"),
            parse("{ \"_id\" : 2, \"item\" : \"jkl\", \"total\" : 21 }"),
            parse("{ \"_id\" : 3, \"item\" : \"xyz\", \"total\" : 5 } }"));
        for (int i = 1; i < 4; i++) {
            compare(i, expected, list);
        }
    }

    private void compare(final int id, final List<Document> expected, final List<Document> actual) {
        assertEquals(find(id, expected), find(id, actual));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Document find(final int id, final List<Document> documents) {
        return documents.stream().filter(d -> d.getInteger("_id").equals(id)).findFirst().get();
    }

    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", new Date()), new User("John Doe", new Date())));

        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .match(getDs().find(User.class)
                                                       .field("name").equal("john doe"));
        assertEquals(1, count(pipeline.execute(User.class)));

        assertEquals(2, count(pipeline.execute(User.class,
            new dev.morphia.aggregation.experimental.AggregationOptions()
                .collation(Collation.builder()
                                    .locale("en")
                                    .collationStrength(SECONDARY)
                                    .build()))));
    }

    @Test
    public void testGenericAccumulatorUsage() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        MorphiaCursor<CountResult> aggregation = getDs().aggregate(Book.class)
                                                        .group(Group.of(id("author")
                                                                   .field("count", sum(literal(1)))))
                                                        .sort(on().ascending("_id"))
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
    public void testAddFields() {
        List<Document> list = List.of(
            parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
            parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        getDatabase().getCollection("scores").insertMany(list);

        List<Document> result = getDs().aggregate(Score.class)
                                       .addFields(AddFields.of()
                                                           .field("totalHomework", sum(field("homework")))
                                                           .field("totalQuiz", sum(field("quiz")))
                                                 )
                                       .addFields(AddFields.of()
                                                           .field("totalScore", add(field("totalHomework"),
                                                               field("totalQuiz"), field("extraCredit"))))
                                       .execute(Document.class)
                                       .toList();

        list = List.of(
            parse("{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
                  + " 'totalQuiz' : 18, 'totalScore' : 43 }"),
            parse("{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
                  + "'totalQuiz' : 16, 'totalScore' : 40 }"));

        assertEquals(list, result);
    }

    @Test
    public void testBucket() {
        List<Document> list = List.of(
            parse("{'_id': 1, 'title': 'The Pillars of Society', 'artist': 'Grosz', 'year': 1926, 'price': NumberDecimal('199.99') }"),
            parse("{'_id': 2, 'title': 'Melancholy III', 'artist': 'Munch', 'year': 1902, 'price': NumberDecimal('280.00') }"),
            parse("{'_id': 3, 'title': 'Dancer', 'artist': 'Miro', 'year': 1925, 'price': NumberDecimal('76.04') }"),
            parse("{'_id': 4, 'title': 'The Great Wave off Kanagawa', 'artist': 'Hokusai', 'price': NumberDecimal('167.30') }"),
            parse("{'_id': 5, 'title': 'The Persistence of Memory', 'artist': 'Dali', 'year': 1931, 'price': NumberDecimal('483.00') }"),
            parse("{'_id': 6, 'title': 'Composition VII', 'artist': 'Kandinsky', 'year': 1913, 'price': NumberDecimal('385.00') }"),
            parse("{'_id': 7, 'title': 'The Scream', 'artist': 'Munch', 'year': 1893}"),
            parse("{'_id': 8, 'title': 'Blue Flower', 'artist': 'O\\'Keefe', 'year': 1918, 'price': NumberDecimal('118.42') }"));

        getDatabase().getCollection("artwork").insertMany(list);

        List<Document> results = getDs().aggregate(Artwork.class).bucket(Bucket.of()
                                                                                  .groupBy(field("price"))
                                                                                  .boundaries(literal(0), literal(200), literal(400))
                                                                                  .defaultValue("Other")
                                                                                  .outputField("count", sum(literal(1)))
                                                                                  .outputField("titles", push().source("title")))
                                           .execute(Document.class)
                                           .toList();

        List<Document> documents = List.of(
            parse("{'_id': 0, 'count': 4, 'titles': ['The Pillars of Society', 'Dancer', 'The Great Wave off Kanagawa', 'Blue Flower']}"),
            parse("{'_id': 200, 'count': 2, 'titles': ['Melancholy III', 'Composition VII']}"),
            parse("{'_id': 'Other', 'count': 2, 'titles': ['The Persistence of Memory', 'The Scream']}"));
        assertEquals(documents, results);
    }

    @Test
    public void testLimit() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        List<Book> aggregate = getDs().aggregate(Book.class)
                                      .limit(2)
                                      .execute(Book.class)
                                      .toList();
        Assert.assertEquals(2, aggregate.size());
    }

    @Test
    public void testLookup() {
        // Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
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

        List<Order> lookups = getDs().aggregate(Order.class)
                                     .lookup(from(Inventory.class)
                                                 .localField("item")
                                                 .foreignField("sku")
                                                 .as("inventoryDocs"))
                                     .sort(on().ascending("_id"))
                                     .execute(Order.class)
                                     .toList();
        Assert.assertEquals(inventories.get(0), lookups.get(0).getInventoryDocs().get(0));
        Assert.assertEquals(inventories.get(3), lookups.get(1).getInventoryDocs().get(0));
        Assert.assertEquals(inventories.get(4), lookups.get(2).getInventoryDocs().get(0));
        Assert.assertEquals(inventories.get(5), lookups.get(2).getInventoryDocs().get(1));
    }

    @Test
    public void testNullGroupId() {
        getDs().save(asList(new User("John", new Date()),
            new User("Paul", new Date()),
            new User("George", new Date()),
            new User("Ringo", new Date())));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .group(Group.of()
                                                     .fields("count", sum(literal(1))));

        Group group = pipeline.getStage("$group");
        Assert.assertNull(group.getId());
        assertEquals(1, group.getFields().size());

        Document execute = pipeline.execute(Document.class).tryNext();
        assertEquals(Integer.valueOf(4), execute.getInteger("count"));
    }

    @Test
    public void testOut() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        AggregationOptions options = new AggregationOptions();
        Aggregation<Book> aggregation = getDs().aggregate(Book.class)
                                               .group(Group.of(id("author"))
                                                          .fields("books", push()
                                                                      .source("title")));
        aggregation.out(Author.class, options);
        Assert.assertEquals(2, getMapper().getCollection(Author.class).countDocuments());
        Author author = aggregation.execute(Author.class).next();
        Assert.assertEquals("Homer", author.getName());
        Assert.assertEquals(asList("The Odyssey", "Iliad"), author.getBooks());

        aggregation.out("different");

        Assert.assertEquals(2, getDatabase().getCollection("different").countDocuments());
    }

    @Test
    public void testProjection() {
        var doc = parse("{\"_id\" : 1, title: \"abc123\", isbn: \"0001122223334\", author: { last: \"zzz\", first: \"aaa\" }, copies: 5,\n"
                        + "  lastModified: \"2016-07-28\"}");

        getDatabase().getCollection("books").insertOne(doc);
        Aggregation<Book> pipeline = getDs().aggregate(Book.class)
                                                  .project(of()
                                                               .include("title")
                                                               .include("author"));
        MorphiaCursor<Document> aggregate = pipeline.execute(Document.class);
        doc = parse("{ \"_id\" : 1, title: \"abc123\", author: { last: \"zzz\", first: \"aaa\" }}");
        Assert.assertEquals(doc, aggregate.next());

        pipeline = getDs().aggregate(Book.class)
                          .project(of()
                                       .supressId()
                                       .include("title")
                                       .include("author"));
        aggregate = pipeline.execute(Document.class);

        doc = parse("{title: \"abc123\", author: { last: \"zzz\", first: \"aaa\" }}");
        Assert.assertEquals(doc, aggregate.next());

        pipeline = getDs().aggregate(Book.class)
                          .project(of()
                                       .exclude("lastModified"));
        aggregate = pipeline.execute(Document.class);

        doc = parse("{\"_id\" : 1, title: \"abc123\", isbn: \"0001122223334\", author: { last: \"zzz\", first: \"aaa\" }, copies: 5}");
        Assert.assertEquals(doc, aggregate.next());
    }

    @Test
    public void testSampleStage() {
        getDs().save(asList(new User("John", new Date()),
            new User("Paul", new Date()),
            new User("George", new Date()),
            new User("Ringo", new Date())));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .sample(Sample.of(1));
        Sample sample = pipeline.getStage("$sample");
        assertEquals(1, sample.getSize());

        List<User> list = pipeline.execute(User.class).toList();
        assertEquals(1, list.size());
    }

/*
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
        Iterator<City> citiesOrderedByDistanceFromLondon = getDs().createAggregation(City.class)
                                                                  .geoNear(GeoNear.builder("distance")
                                                                                  .setNear(londonPoint)
                                                                                  .setSpherical(true)
                                                                                  .build())
                                                                  .aggregate(City.class).iterator();

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
                                                                                .aggregate(PlaceWithLegacyCoords.class)
                                                                                .iterator();

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
        City london = new City("London", new Point(new Position(latitude, longitude)));
        getDs().save(london);
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<City> citiesOrderedByDistanceFromLondon = getDs().createAggregation(City.class)
                                                                  .geoNear(GeoNear.builder("distance")
                                                                                  .setNear(latitude, longitude)
                                                                                  .setSpherical(true)
                                                                                  .build())
                                                                  .aggregate(City.class)
                                                                  .iterator();

        // then
        Assert.assertTrue(citiesOrderedByDistanceFromLondon.hasNext());
        Assert.assertEquals(london, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(manchester, citiesOrderedByDistanceFromLondon.next());
        Assert.assertEquals(sevilla, citiesOrderedByDistanceFromLondon.next());
        Assert.assertFalse(citiesOrderedByDistanceFromLondon.hasNext());
    }


    @Test
    public void testSizeProjection() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        final AggregationPipeline pipeline = getDs().createAggregation(Book.class)
                                                    .group("author", grouping("titles", addToSet("title")))
                                                    .project(projection("_id").suppress(),
                                                        projection("author", "_id"),
                                                        projection("copies", size(projection("titles"))))
                                                    .sort(ascending("author"));
        Iterator<Book> aggregate = pipeline.aggregate(Book.class).iterator();
        Book book = aggregate.next();
        Assert.assertEquals("Dante", book.author);
        Assert.assertEquals(3, book.copies.intValue());

        final List<Document> stages = ((AggregationPipelineImpl) pipeline).getStages();
        Assert.assertEquals(stages.get(0), obj("$group", obj("_id", "$author").append("titles", obj("$addToSet", "$title"))));
        Assert.assertEquals(stages.get(1), obj("$project", obj("_id", 0)
                                                               .append("author", "$_id")
                                                               .append("copies", obj("$size", "$titles"))));
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
                           .aggregate(Book.class)
                           .first();
        Assert.assertEquals("Eclogues", book.title);
        Assert.assertEquals("Dante", book.author);
        Assert.assertEquals(2, book.copies.intValue());
    }

    @Test
    public void testUnwind() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        getDs().save(asList(new User("jane", format.parse("2011-03-02"), "golf", "racquetball"),
            new User("joe", format.parse("2012-07-02"), "tennis", "golf", "swimming"),
            new User("john", format.parse("2012-07-02"))));

        Iterator<User> aggregate = getDs().createAggregation(User.class)
                                          .project(projection("_id").suppress(), projection("name"), projection("joined"),
                                              projection("likes"))
                                          .unwind("likes")
                                          .aggregate(User.class)
                                          .iterator();
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

        aggregate = getDs().createAggregation(User.class)
                           .project(projection("_id").suppress(), projection("name"), projection("joined"),
                               projection("likes"))
                           .unwind("likes", new UnwindOptions().preserveNullAndEmptyArrays(true))
                           .aggregate(User.class)
                           .iterator();
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
    public void testSortByCount() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));


        Iterator<SortByCountResult> aggregate = getDs().createAggregation(Book.class)
                                                       .sortByCount("author").aggregate(SortByCountResult.class)
                                                       .iterator();
        SortByCountResult result1 = aggregate.next();
        assertEquals(result1.id, "Dante");
        assertEquals(result1.count, 3);

        SortByCountResult result2 = aggregate.next();
        assertEquals(result2.id, "Homer");
        assertEquals(result2.count, 2);

    }

    @Test
    public void testBucketWithoutOptions() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<BucketResult> aggregate = getDs().createAggregation(Book.class)
                                                  .bucket("copies", Arrays.asList(1, 5, 12)).aggregate(BucketResult.class)
                                                  .iterator();
        BucketResult result1 = aggregate.next();
        assertEquals(result1.id, "1");
        assertEquals(result1.count, 3);

        BucketResult result2 = aggregate.next();
        assertEquals(result2.id, "5");
        assertEquals(result2.count, 2);

    }

    @Test
    public void testBucketWithOptions() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Iterator<BucketResult> aggregate = getDs().createAggregation(Book.class)
                                                  .bucket("copies", Arrays.asList(1, 5, 10),
                                                      new BucketOptions()
                                                          .defaultField("test")
                                                          .output("count").sum(1))
                                                  .aggregate(BucketResult.class)
                                                  .iterator();
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

        getDs().createAggregation(Book.class)
               .bucket("copies", Arrays.asList(5, 1, 10),
                   new BucketOptions()
                       .defaultField("test")
                       .output("count")
                       .sum(1))
               .aggregate(BucketResult.class)
               .iterator();

    }

    @Test(expected = RuntimeException.class)
    public void testBucketWithBoundariesWithSizeLessThanTwo() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        getDs().createAggregation(Book.class)
               .bucket("copies", singletonList(10),
                   new BucketOptions()
                       .defaultField("test")
                       .output("count")
                       .sum(1))
               .aggregate(BucketResult.class)
               .iterator();

    }


    @Test
    public void testBucketAutoWithoutGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5),
            new Book("Divine Comedy", "Dante", 10),
            new Book("Eclogues", "Dante", 40),
            new Book("The Odyssey", "Homer", 21)));

        Iterator<BucketAutoResult> aggregate = getDs().createAggregation(Book.class)
                                                      .bucketAuto("copies", 2)
                                                      .aggregate(BucketAutoResult.class)
                                                      .iterator();
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

    @Test
    public void testBucketAutoWithGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5),
            new Book("Divine Comedy", "Dante", 7),
            new Book("Eclogues", "Dante", 40),
            new Book("The Odyssey", "Homer", 21)));

        Iterator<BooksBucketResult> aggregate = getDs().createAggregation(Book.class)
                                                       .bucketAuto("copies", 3,
                                                           new BucketAutoOptions()
                                                               .granularity(BucketAutoOptions.Granularity.POWERSOF2)
                                                               .output("authors")
                                                               .addToSet("author")
                                                               .output("count")
                                                               .sum(1))
                                                       .aggregate(BooksBucketResult.class)
                                                       .iterator();
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
    public void testUserPreferencesPipeline() {
        final AggregationPipeline pipeline = getDs().createAggregation(Book.class)  // the class is irrelevant for this test
                                                    .group("state", Group.grouping("total_pop", sum("pop")))
                                                    .match(getDs().find(Book.class)
                                                                  .disableValidation()
                                                                  .field("total_pop").greaterThanOrEq(10000000));
        Document group = obj("$group", obj("_id", "$state")
                                           .append("total_pop", obj("$sum", "$pop")));

        Document match = obj("$match", obj("total_pop", obj("$gte", 10000000)));

        final List<Document> stages = ((AggregationPipelineImpl) pipeline).getStages();
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
        List<Document> stages = ((AggregationPipelineImpl) pipeline).getStages();
        Document group = stages.get(0);
        Document addToSet = getDocument(group, "$group", "messageDataSet", "$addToSet");
        Assert.assertNotNull(addToSet);
        Assert.assertEquals(addToSet.get("sentDate"), "$sentDate");
        Assert.assertEquals(addToSet.get("messageId"), "$_id");
    }

*/

    @Entity("scores")
    private static class Score {
        @Id
        private ObjectId id;
    }

    @Entity
    public static class Artwork {
        @Id
        private ObjectId id;
    }
}
