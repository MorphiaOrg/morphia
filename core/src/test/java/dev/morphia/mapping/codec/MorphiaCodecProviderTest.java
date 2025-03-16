package dev.morphia.mapping.codec;

import java.util.List;

import dev.morphia.DatastoreImpl;
import dev.morphia.test.TestBase;

import org.bson.codecs.pojo.PropertyCodecProvider;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class MorphiaCodecProviderTest extends TestBase {

    @Test
    public void ensureCustomCodedProvidersComeFirst() {
        // given a custom 'no-op' codec is provided in the service configuration
        // (see META-INF/services/dev.morphia.mapping.codec.MorphiaPropertyCodecProvider)
        // when we instantiate a morphia codec provider
        DatastoreImpl datastore = getDs();
        MorphiaCodecProvider provider = new MorphiaCodecProvider(datastore);

        // then we expect that the custom provider we provided is the first codec in the list
        List<PropertyCodecProvider> providers = provider.getPropertyCodecProviders();
        assertEquals(providers.size(), 3);
        assertTrue(providers.get(0) instanceof NoOpMorphiaPropertyCodecProvider);
    }
}
