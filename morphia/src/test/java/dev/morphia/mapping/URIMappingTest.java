package dev.morphia.mapping;


import dev.morphia.annotations.Entity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author ScottHernandez
 */
public class URIMappingTest extends TestBase {

    @Test
    public void testURIField() throws URISyntaxException {
        final ContainsURI entity = new ContainsURI();
        final URI testURI = new URI("http://lamest.local/test.html");

        entity.uri = testURI;
        getDs().save(entity);
        final ContainsURI loaded = getDs().find(ContainsURI.class).execute(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(loaded.uri);
        Assert.assertEquals(testURI, loaded.uri);

    }

    @Test
    public void testURIMap() throws URISyntaxException {
        final ContainsURIKeyedMap entity = new ContainsURIKeyedMap();
        final URI testURI = new URI("http://lamest.local/test.html");

        entity.uris.put(testURI, "first");
        getDs().save(entity);
        final ContainsURIKeyedMap loaded = getDs().find(ContainsURIKeyedMap.class).execute(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(loaded.uris);
        Assert.assertEquals(1, loaded.uris.size());
        Assert.assertEquals(testURI, loaded.uris.keySet().iterator().next());

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
        private final Map<URI, String> uris = new HashMap<URI, String>();
    }
}
