package dev.morphia.mapping.lazy;

import dev.morphia.TestBase;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import org.junit.Assert;

public class ProxyTestBase extends TestBase {
    protected void assertIsProxy(final Object p) {
        Assert.assertTrue("Should be a proxy: " + p.getClass(), p instanceof MorphiaProxy);
    }

    protected void assertFetched(final Object e) {
        Assert.assertTrue(isFetched(e));
    }

    protected void assertNotFetched(final Object e) {
        Assert.assertFalse(isFetched(e));
    }

    private MorphiaProxy asMorphiaProxy(final Object e) {
        return (MorphiaProxy) e;
    }

    private boolean isFetched(final Object e) {
        return asMorphiaProxy(e).isFetched();
    }
}
