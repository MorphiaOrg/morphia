package dev.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;

import java.io.Serializable;


/**
 * @author scott hernandez
 */
public class AnonymousClassTest extends TestBase {

    @Test
    public void testDelete() throws Exception {
        final E e = new E();
        e.id = new CId("test");

        final Key<E> key = getDs().save(e);
        getDs().delete(E.class, e.id);
    }

    @Test
    public void testMapping() throws Exception {
        E e = new E();
        e.id = new CId("test");

        getDs().save(e);
        e = getDs().get(e);
        Assert.assertEquals("test", e.id.name);
        Assert.assertNotNull(e.id.id);
    }

    @Test
    public void testOtherDelete() throws Exception {
        final E e = new E();
        e.id = new CId("test");

        getDs().save(e);
        getAds().delete(getDs().getCollection(E.class).getName(), E.class, e.id);
    }

    @Embedded
    private static class CId implements Serializable {
        private final ObjectId id = new ObjectId();
        private String name;

        CId() {
        }

        CId(final String n) {
            name = n;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof CId)) {
                return false;
            }
            final CId other = ((CId) obj);
            return other.id.equals(id) && other.name.equals(name);
        }

    }

    private static class E {
        @Id
        private CId id;
        private String e;
    }

}
