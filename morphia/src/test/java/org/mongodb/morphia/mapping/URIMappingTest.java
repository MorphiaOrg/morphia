package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * @author ScottHernandez
 */
public class URIMappingTest extends TestBase {

    @Test
    public void testURIField() throws Exception {
        final ContainsURI entity = new ContainsURI();
        final URI testURI = new URI("http://lamest.local/test.html");

        entity.uri = testURI;
        getDs().save(entity);
        final ContainsURI loaded = getDs().find(ContainsURI.class).get();
        Assert.assertNotNull(loaded.uri);
        Assert.assertEquals(testURI, loaded.uri);

    }

    @Test
    public void testURIMap() throws Exception {
        final ContainsURIKeyedMap entity = new ContainsURIKeyedMap();
        final URI testURI = new URI("http://lamest.local/test.html");

        entity.uris.put(testURI, "first");
        getDs().save(entity);
        final ContainsURIKeyedMap loaded = getDs().find(ContainsURIKeyedMap.class).get();
        Assert.assertNotNull(loaded.uris);
        Assert.assertEquals(1, loaded.uris.size());
        Assert.assertEquals(testURI, loaded.uris.keySet().iterator().next());

    }

    private static class ContainsURI {
        @Id
        private ObjectId id;
        private URI uri;
    }

    private static class ContainsURIKeyedMap {
        private final Map<URI, String> uris = new HashMap<URI, String>();
        @Id
        private ObjectId id;
    }
}
