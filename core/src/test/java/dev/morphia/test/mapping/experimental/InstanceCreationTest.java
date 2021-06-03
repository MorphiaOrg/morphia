package dev.morphia.test.mapping.experimental;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class InstanceCreationTest extends TestBase {
    @Test
    public void basicReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).iterator(new FindOptions()
                                                                      .limit(1)).tryNext();
        assertEquals(author, loaded);
    }

    private static class BaseEntity {
        @Id
        protected ObjectId id;

        public ObjectId getId() {
            return id;
        }
    }

    @Entity
    private static class Author extends BaseEntity {

        public Author(String name) {
            this.name = name;
        }

        private final String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Author author = (Author) o;

            if (id != null ? !id.equals(author.id) : author.id != null) {
                return false;
            }
            return name != null ? name.equals(author.name) : author.name == null;
        }

        @Override
        public String toString() {
            return "Author{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
