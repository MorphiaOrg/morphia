package org.mongodb.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.testutil.TestEntity;


@Ignore
public class TestCGLibLazyProxyFactory extends ProxyTestBase {
    @Test
    @Ignore
    public final void testCreateProxy() {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        final E e = new E();
        e.setFoo("bar");
        final Key<E> key = getDs().save(e);
        E eProxy = new CGLibLazyProxyFactory().createProxy(getDs(), E.class, key);

        assertNotFetched(eProxy);
        Assert.assertEquals("bar", eProxy.getFoo());
        assertFetched(eProxy);

        eProxy = deserialize(eProxy);
        assertNotFetched(eProxy);
        Assert.assertEquals("bar", eProxy.getFoo());
        assertFetched(eProxy);

    }

    public static class E extends TestEntity {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String string) {
            foo = string;
        }
    }

}
