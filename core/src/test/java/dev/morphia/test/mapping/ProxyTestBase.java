package dev.morphia.test.mapping;

import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.test.TestBase;

import org.junit.jupiter.api.Assertions;

public class ProxyTestBase extends TestBase {
    protected void assertFetched(Object e) {
        Assertions.assertTrue(isFetched(e));
    }

    protected void assertIsProxy(Object p) {
        Assertions.assertTrue(p instanceof MorphiaProxy, "Should be a proxy: " + p.getClass());
    }

    protected void assertNotFetched(Object e) {
        Assertions.assertFalse(isFetched(e));
    }

    private MorphiaProxy asMorphiaProxy(Object e) {
        return (MorphiaProxy) e;
    }

    private boolean isFetched(Object e) {
        return asMorphiaProxy(e).isFetched();
    }
}
