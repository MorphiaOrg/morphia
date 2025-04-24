package dev.morphia.test.mapping;

import dev.morphia.test.TestBase;
import dev.morphia.test.models.Author;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class InstanceCreationTest extends TestBase {
    @Test
    public void basicReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).iterator().tryNext();
        assertEquals(author, loaded);
    }
}
