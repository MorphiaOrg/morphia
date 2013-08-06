package com.google.code.morphia.issue502;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.mongodb.DBObject;

/**
 * Can't inherit HashSet : generic is lost...
 *
 */
public class CollectionInheritanceTest extends TestBase {

	/** Real test */
	@Test
	public void testSavingBook() throws Exception {
		// Test saving
		ds.save(newBook());

		assertEquals(1, ds.getCollection(Book.class).count());
	}

	/** Issue's details... */
	@Test
	public void testMappingBook() throws Exception {
		// Mapping...
		morphia.map(Book.class /* , Authors.class, Author.class */);

		// Test mapping : author objects must be converted into DBObject (but wasn't)
		final DBObject dbBook = morphia.getMapper().toDBObject(newBook());
		final Object firstBook = ((List<?>) dbBook.get("authors")).iterator().next();
		assertTrue("Author wasn't converted : expected instanceof <DBObject>, but was <" + firstBook.getClass() + ">",
				firstBook instanceof DBObject);

	}

	private static Book newBook() {
		final Book book = new Book();
		book.authors.add(new Author("Hergé"));
		book.authors.add(new Author("George R. R. Martin"));
		return book;
	}

	@SuppressWarnings("unused")
	private static class Author {
		private String name;

		public Author(String name) {
			this.name = name;
		}

	}

	@SuppressWarnings("serial")
	private static class Authors extends HashSet<Author> {
	}

	private static class Book {
		@Id
		private ObjectId id;

		private Authors authors = new Authors();
	}
}
