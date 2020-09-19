package dev.morphia.mapping.lazy;

import dev.morphia.TestBase;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import org.junit.Assert;

public class ProxyTestBase extends TestBase {
    protected void assertIsProxy(Object p) {
        Assert.assertTrue("Should be a proxy: " + p.getClass(), p instanceof MorphiaProxy);
    }

    protected void assertFetched(Object e) {
        Assert.assertTrue(isFetched(e));
    }

    protected void assertNotFetched(Object e) {
        Assert.assertFalse(isFetched(e));
    }

    private MorphiaProxy asMorphiaProxy(Object e) {
        return (MorphiaProxy) e;
    }

    private boolean isFetched(Object e) {
        return asMorphiaProxy(e).isFetched();
    }
}
