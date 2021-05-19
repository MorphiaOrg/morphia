package dev.morphia.test.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestUriMapping extends TestBase {

    @Test
    public void testURIField() throws URISyntaxException {
        final ContainsURI entity = new ContainsURI();
        final URI testURI = new URI("http://lamest.local/test.html");

        entity.uri = testURI;
        getDs().save(entity);
        final ContainsURI loaded = getDs().find(ContainsURI.class).iterator(new FindOptions().limit(1)).tryNext();
        assertNotNull(loaded.uri);
        assertEquals(testURI, loaded.uri);

    }

    @Test
    public void testURIMap() throws URISyntaxException {
        final ContainsURIKeyedMap entity = new ContainsURIKeyedMap();
        final URI testURI = new URI("http://lamest.local/test.html");

        entity.uris.put(testURI, "first");
        getDs().save(entity);
        final ContainsURIKeyedMap loaded = getDs().find(ContainsURIKeyedMap.class).iterator(new FindOptions().limit(1)).tryNext();
        assertNotNull(loaded.uris);
        assertEquals(loaded.uris.size(), 1);
        assertEquals(testURI, loaded.uris.keySet().iterator().next());

    }

    @Entity
    private static class ContainsURI {
        @Id
        private ObjectId id;
        private URI uri;
    }

    @Entity
    private static class ContainsURIKeyedMap {
        @Id
        private ObjectId id;
        private final Map<URI, String> uris = new HashMap<>();
    }
}
