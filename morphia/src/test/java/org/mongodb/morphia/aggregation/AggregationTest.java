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

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.MorphiaIterator;

import java.util.Arrays;
import java.util.List;

public class AggregationTest extends TestBase {
    @Test
    public void testOut() {
        getDs().save(new Book("The Banquet", "Dante", 2),
                     new Book("Divine Comedy", "Dante", 1),
                     new Book("Eclogues", "Dante", 2),
                     new Book("The Odyssey", "Homer", 10),
                     new Book("Iliad", "Homer", 10));

        MorphiaIterator<Author, Author> aggregate = getDs().createAggregation(Book.class, Author.class)
                                                        .group("author", Group.grouping("books", Group.push("title")))
                                                        .out()
                                                        .aggregate();
        Assert.assertEquals(2, getDs().getCollection(Author.class).count());
        Author author = aggregate.next();
        Assert.assertEquals("Homer", author.getName());
        Assert.assertEquals(Arrays.asList("The Odyssey", "Iliad"), author.getBooks());

        getDs().createAggregation(Book.class, Author.class)
            .group("author", Group.grouping("books", Group.push("title")))
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

    @Entity("books")
    private static class Book {
        @Id
        private ObjectId id;
        private String title;
        private String author;
        private Integer copies;

        private Book() {
        }

        private Book(final String title, final String author, final Integer copies) {
            this.title = title;
            this.author = author;
            this.copies = copies;
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
}
