package dev.morphia;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.TestMapping.BaseEntity;
import dev.morphia.annotations.Entity;
import dev.morphia.query.Query;

import java.util.List;


/**
 * Test from list, but doesn't seems to be a problem. Here as an example.
 */
public class TestLargeObjectsWithCursor extends TestBase {
    private int documentsNb;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        getMorphia().map(E.class);
        documentsNb = 1000;
        for (int i = 0; i < documentsNb; i++) {
            getDs().save(new E(i));
        }
    }

    @Test
    public void testWithManyElementsInCollection() {
        Query<E> query = getDs().find(E.class);
        final long countAll = query.count();
        query = getDs().find(E.class);
        final List<E> list = toList(query.find());
        Assert.assertEquals(documentsNb, countAll);
        Assert.assertEquals(documentsNb, list.size());
    }

    @Entity
    public static class E extends BaseEntity {
        private final Integer index;
        private final byte[] largeContent;

        public E() {
            index = null;
            largeContent = null;
        }

        public E(final int i) {
            index = i;
            largeContent = createLargeByteArray();
        }

        public Integer getIndex() {
            return index;
        }

        private byte[] createLargeByteArray() {
            final int size = (int) (4000 + Math.random() * 10000);
            final byte[] arr = new byte[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = 'a';
            }
            return arr;
        }
    }
}
