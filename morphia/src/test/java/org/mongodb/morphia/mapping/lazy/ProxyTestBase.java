package org.mongodb.morphia.mapping.lazy;


import org.junit.Assert;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class ProxyTestBase extends TestBase {

    protected ProxiedReference asProxiedReference(final Object e) {
        return (ProxiedReference) e;
    }

    protected void assertFetched(final Object e) {
        Assert.assertTrue(isFetched(e));
    }

    protected void assertIsProxy(final Object p) {
        Assert.assertTrue("Should be a proxy", p instanceof ProxiedReference);
    }

    protected void assertNotFetched(final Object e) {
        Assert.assertFalse(isFetched(e));
    }

    protected void assertNotProxy(final Object p) {
        Assert.assertFalse("Should not be a proxy", p instanceof ProxiedReference);
    }

    protected <T> T deserialize(final Object t) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(t);
            os.close();
            final byte[] ba = baos.toByteArray();

            return (T) new ObjectInputStream(new ByteArrayInputStream(ba)).readObject();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isFetched(final Object e) {
        return asProxiedReference(e).__isFetched();
    }

    protected Object unwrap(final Object proxy) {
        return proxy instanceof ProxiedReference
               ? ((ProxiedReference) proxy).__unwrap()
               : proxy;
    }

    protected List unwrapList(final List list) {
        if (list == null) {
            return null;
        }

        final List unwrapped = new ArrayList();
        for (Object entry : list) {
            unwrapped.add(unwrap(entry));
        }

        return unwrapped;
    }

    protected <K, V> Map<K, V> unwrapMap(final Map<K, V> map) {
        if (map == null) {
            return null;
        }

        final Map unwrapped = new LinkedHashMap();
        for (Map.Entry entry : map.entrySet()) {
            unwrapped.put(entry.getKey(), unwrap(entry.getValue()));
        }

        return unwrapped;
    }

}
