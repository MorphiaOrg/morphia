package com.google.code.morphia.mapping;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import junit.framework.Assert;


/**
 * @author ScottHernandez
 */
public class URIMappingTest extends TestBase {

  private static class ContainsURI {
    @Id ObjectId id;
    URI uri;
  }

  private static class ContainsURIKeyedMap {
    @Id ObjectId id;
    final Map<URI, String> uris = new HashMap<URI, String>();
  }


  @Test
  public void testURIField() throws Exception {
    final ContainsURI entity = new ContainsURI();
    final URI testURI = new URI("http://lamest.local/test.html");

    entity.uri = testURI;
    ds.save(entity);
    final ContainsURI loaded = ds.find(ContainsURI.class).get();
    Assert.assertNotNull(loaded.uri);
    Assert.assertEquals(testURI, loaded.uri);

  }

  @Test
  public void testURIMap() throws Exception {
    final ContainsURIKeyedMap entity = new ContainsURIKeyedMap();
    final URI testURI = new URI("http://lamest.local/test.html");

    entity.uris.put(testURI, "first");
    ds.save(entity);
    final ContainsURIKeyedMap loaded = ds.find(ContainsURIKeyedMap.class).get();
    Assert.assertNotNull(loaded.uris);
    Assert.assertEquals(1, loaded.uris.size());
    Assert.assertEquals(testURI, loaded.uris.keySet().iterator().next());

  }
}
