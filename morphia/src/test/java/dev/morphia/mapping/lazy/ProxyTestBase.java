package dev.morphia.mapping.lazy;


import dev.morphia.mapping.codec.references.MorphiaProxy;
import org.junit.Assert;
import dev.morphia.TestBase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class ProxyTestBase extends TestBase {

    private MorphiaProxy asMorphiaProxy(final Object e) {
        return (MorphiaProxy) e;
    }

    protected void assertFetched(final Object e) {
        Assert.assertTrue(isFetched(e));
    }

    protected void assertIsProxy(final Object p) {
        Assert.assertTrue("Should be a proxy: " + p.getClass(), p instanceof MorphiaProxy);
    }

    protected void assertNotFetched(final Object e) {
        Assert.assertFalse(isFetched(e));
    }

    protected void assertNotProxy(final Object p) {
        Assert.assertFalse("Should not be a proxy: " + p.getClass(), p instanceof MorphiaProxy);
    }

    private boolean isFetched(final Object e) {
        return asMorphiaProxy(e).isFetched();
    }
}
