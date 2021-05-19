package dev.morphia.test.mapping.lazy;

import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.test.TestBase;
import org.testng.Assert;

public class ProxyTestBase extends TestBase {
    protected void assertFetched(Object e) {
        Assert.assertTrue(isFetched(e));
    }

    protected void assertIsProxy(Object p) {
        Assert.assertTrue(p instanceof MorphiaProxy, "Should be a proxy: " + p.getClass());
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
