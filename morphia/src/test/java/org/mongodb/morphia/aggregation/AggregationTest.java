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
import com.mongodb.DBCursor;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.push;
import static org.mongodb.morphia.aggregation.Group.sum;
import static org.mongodb.morphia.aggregation.Projection.divide;
import static org.mongodb.morphia.aggregation.Projection.projection;

public class AggregationTest extends TestBase {
    @Test
    public void testOut() {
        checkMinServerVersion(2.6);
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

        AggregationOptions options = AggregationOptions.builder()
                                                       .outputMode(AggregationOptions.OutputMode.CURSOR)
                                                       .build();
        Iterator<Author> aggregate = getDs().createAggregation(Book.class)
                                            .group("author", grouping("books", push("title")))
                                            .out(Author.class, options);
        Assert.assertEquals(2, getDs().getCollection(Author.class).count());
        Author author = aggregate.next();
        Assert.assertEquals("Homer", author.name);
        Assert.assertEquals(Arrays.asList("The Odyssey", "Iliad"), author.books);

        getDs().createAggregation(Book.class)
               .group("author", grouping("books", push("title")))
               .out("different", Author.class);

        Assert.assertEquals(2, getDb().getCollection("different").count());
    }

    @Test
    public void testOutNamedCollection() {
        checkMinServerVersion(2.6);
        getDs().save(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
                     new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
                     new Book("Eclogues", "Dante", 2, "Italian", ""),
                     new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
                     new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel"));

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
    public void testLimit() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

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

    @Test
    public void testSkip() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

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
        getDs().save(new User("jane", format.parse("2011-03-02"), "golf", "racquetball"),
                     new User("joe", format.parse("2012-07-02"), "tennis", "golf", "swimming"));

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
                    Assert.fail("Should only find 5 elements");
            }
            count++;
        }
    }

    @Test
    public void testProjection() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

        Iterator<Book> aggregate = getDs().createAggregation(Book.class)
                                          .group("author", grouping("copies", sum("copies")))
                                          .project(projection("_id").suppress(),
                                                   projection("author", "_id"),
                                                   projection("copies", divide(projection("copies"), 5)))
                                          .sort(Sort.ascending("author"))
                                          .aggregate(Book.class);
        Book book = aggregate.next();
        Assert.assertEquals("Dante", book.author);
        Assert.assertEquals(1, book.copies.intValue());
    }

    @Test
    public void testGenericAccumulatorUsage() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

        Iterator<CountResult> aggregation = getDs().createAggregation(Book.class)
                                                   .group("author", grouping("count", new Accumulator("$sum", 1)))
                                                   .sort(Sort.ascending("_id"))
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
    public void testGeoNear() {
        // Given


        // When

        // Then
    }

    @Entity(value = "books", noClassnameStored = true)
    private static final class Book {
        @Id
        private ObjectId id;
        private String title;
        private String author;
        private Integer copies;
        private List<String> tags;

        private Book() {
        }

        private Book(final String title, final String author, final Integer copies, final String... tags) {
            this.title = title;
            this.author = author;
            this.copies = copies;
            this.tags = Arrays.asList(tags);
        }

        @Override
        public String toString() {
            return String.format("Book{title='%s', author='%s', copies=%d, tags=%s}", title, author, copies, tags);
        }
    }

    @Entity("authors")
    private static class Author {
        @Id
        private String name;
        private List<String> books;
    }

    @Entity("users")
    private static final class User {
        @Id
        private ObjectId id;
        private String name;
        private Date joined;
        private List<String> likes;

        private User() {
        }

        private User(final String name, final Date joined, final String... likes) {
            this.name = name;
            this.joined = joined;
            this.likes = Arrays.asList(likes);
        }

        @Override
        public String toString() {
            return String.format("User{name='%s', joined=%s, likes=%s}", name, joined, likes);
        }
    }

    @Entity
    private static class CountResult {

        @Id
        private String author;
        private int count;

        public String getAuthor() {
            return author;
        }

        public int getCount() {
            return count;
        }
    }
}
