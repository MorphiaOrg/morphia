package org.mongodb.morphia.mapping.lazy;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Ignore
public class TestReferenceMap extends ProxyTestBase {
    @Test
    public final void testCreateProxy() {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        A a = new A();
        final B b1 = new B();
        final B b2 = new B();

        a.bs.put("b1", b1);
        a.bs.put("b1+", b1);
        a.bs.put("b2", b2);

        getDs().save(b2, b1, a);
        a = getDs().get(a);

        assertIsProxy(a.bs);
        assertNotFetched(a.bs);
        Assert.assertEquals(3, a.bs.size());
        assertFetched(a.bs);

        final B b1read = a.bs.get("b1");
        Assert.assertNotNull(b1read);

        Assert.assertEquals(b1, a.bs.get("b1"));
        Assert.assertEquals(b1, a.bs.get("b1+"));
        // currently fails:
        // assertSame(a.bs.get("b1"), a.bs.get("b1+"));
        Assert.assertNotNull(a.bs.get("b2"));

        a = deserialize(a);
        assertNotFetched(a.bs);
        Assert.assertEquals(3, a.bs.size());
        assertFetched(a.bs);
        Assert.assertEquals(b1, a.bs.get("b1"));
        Assert.assertEquals(b1, a.bs.get("b1+"));
        Assert.assertNotNull(a.bs.get("b2"));

        // make sure, saving does not fetch
        a = deserialize(a);
        assertNotFetched(a.bs);
        getDs().save(a);
        assertNotFetched(a.bs);
    }


    public static class A extends TestEntity {
        @Reference(lazy = true)
        private final Map<String, B> bs = new HashMap<String, B>();
    }

    public static class B implements Serializable {
        @Id
        private final String id = new ObjectId().toString();

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final B other = (B) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            return true;
        }

    }

}
