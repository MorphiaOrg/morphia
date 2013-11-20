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

import com.mongodb.DBCursor;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.logging.Logr;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.query.MorphiaIterator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.push;
import static org.mongodb.morphia.aggregation.Group.sum;
import static org.mongodb.morphia.aggregation.Projection.projection;

public class AggregationTest extends TestBase {
    private static final Logr LOG = MorphiaLoggerFactory.get(AggregationTest.class);

    @Test
    public void testOut() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

        MorphiaIterator<Author, Author> aggregate = getDs().createAggregation(Book.class, Author.class)
                                                        .group("author", grouping("books", push("title")))
                                                        .out()
                                                        .aggregate();
        Assert.assertEquals(2, getDs().getCollection(Author.class).count());
        Author author = aggregate.next();
        Assert.assertEquals("Homer", author.getName());
        Assert.assertEquals(Arrays.asList("The Odyssey", "Iliad"), author.getBooks());

        getDs().createAggregation(Book.class, Author.class)
            .group("author", grouping("books", push("title")))
            .out("different")
            .aggregate();

        Assert.assertEquals(2, getDb().getCollection("different").count());
    }

    @Test
    public void testLimit() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

        MorphiaIterator<Book, Book> aggregate = getDs().createAggregation(Book.class, Book.class)
                                                    .limit(2)
                                                    .aggregate();
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

        MorphiaIterator<Book, Book> aggregate = getDs().createAggregation(Book.class, Book.class)
                                                    .skip(2)
                                                    .aggregate();
        Book book = aggregate.next();
        Assert.assertEquals("Eclogues", book.getTitle());
        Assert.assertEquals("Dante", book.getAuthor());
        Assert.assertEquals(2, book.getCopies().intValue());
    }

    @Test
    public void testUnwind() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        getDs().save(new User("jane", format.parse("2011-03-02"), "golf", "racquetball"),
                     new User("joe", format.parse("2012-07-02"), "tennis", "golf", "swimming"));

        MorphiaIterator<User, User> aggregate = getDs().createAggregation(User.class, User.class)
                                                    .project(projection("_id").suppress(), projection("name"), projection("joined"),
                                                             projection("likes"))
                                                    .unwind("likes")
                                                    .aggregate();
        int count = 0;
        while (aggregate.hasNext()) {
            User user = aggregate.next();
            switch (count) {
                case 0:
                    Assert.assertEquals("jane", user.getName());
                    Assert.assertEquals("golf", user.getLikes().get(0));
                    break;
                case 1:
                    Assert.assertEquals("jane", user.getName());
                    Assert.assertEquals("racquetball", user.getLikes().get(0));
                    break;
                case 2:
                    Assert.assertEquals("joe", user.getName());
                    Assert.assertEquals("tennis", user.getLikes().get(0));
                    break;
                case 3:
                    Assert.assertEquals("joe", user.getName());
                    Assert.assertEquals("golf", user.getLikes().get(0));
                    break;
                case 4:
                    Assert.assertEquals("joe", user.getName());
                    Assert.assertEquals("swimming", user.getLikes().get(0));
                    break;
                default:
                    Assert.fail("Should only find 5 elements");
            }
            count++;
        }
    }

    @Test
    public void testMatch() {
        getDs().save(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
                     new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
                     new Book("Eclogues", "Dante", 2, "Italian", ""),
                     new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
                     new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel"));

        MorphiaIterator<Book, Book> aggregate = getDs().createAggregation(Book.class, Book.class)
                                                    .match(getDs().getQueryFactory().createQuery(getDs())
                                                               .field("author").equal("Homer"))
                                                    .group("author", Group.grouping("copies", sum("copies")))
                                                    .out("testAverage")
                                                    .aggregate();
        DBCursor testAverage = getDb().getCollection("testAverage").find();
        Assert.assertNotNull(testAverage);
        try {
            Assert.assertEquals(20, testAverage.next().get("copies"));
        } finally {
            testAverage.close();
        }
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

        private ObjectId getId() {
            return id;
        }

        private void setId(final ObjectId id) {
            this.id = id;
        }

        private String getTitle() {
            return title;
        }

        private void setTitle(final String title) {
            this.title = title;
        }

        private String getAuthor() {
            return author;
        }

        private void setAuthor(final String author) {
            this.author = author;
        }

        private Integer getCopies() {
            return copies;
        }

        private void setCopies(final Integer copies) {
            this.copies = copies;
        }

        private List<String> getTags() {
            return tags;
        }

        private void setTags(final List<String> tags) {
            this.tags = tags;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Book{");
            sb.append("title='").append(title).append('\'');
            sb.append(", author='").append(author).append('\'');
            sb.append(", copies=").append(copies);
            sb.append(", tags=").append(tags);
            sb.append('}');
            return sb.toString();
        }
    }

    @Entity("authors")
    private static class Author {
        @Id
        private String name;
        private List<String> books;

        private String getName() {
            return name;
        }

        private void setName(final String name) {
            this.name = name;
        }

        private List<String> getBooks() {
            return books;
        }

        private void setBooks(final List<String> books) {
            this.books = books;
        }
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

        private String getName() {
            return name;
        }

        private void setName(final String name) {
            this.name = name;
        }

        private Date getJoined() {
            return joined;
        }

        private void setJoined(final Date joined) {
            this.joined = joined;
        }

        private List<String> getLikes() {
            return likes;
        }

        private void setLikes(final List<String> likes) {
            this.likes = likes;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("User{");
            sb.append("name='").append(name).append('\'');
            sb.append(", joined=").append(joined);
            sb.append(", likes=").append(likes);
            sb.append('}');
            return sb.toString();
        }
    }
}
