package org.mongodb.morphia;


import category.Slow;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.TestMapping.BaseEntity;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.query.Query;

import java.util.List;


/**
 * Test from list, but doesn't seems to be a problem. Here as an example.
 */
public class TestLargeObjectsWithCursor extends TestBase {
    private int documentsNb;

    @Entity
    public static class E extends BaseEntity {
        private final Integer index;
        private final byte[] largeContent;

        public E() {
            index = null;
            largeContent = null;
        }

        private byte[] createLargeByteArray() {
            final int size = (int) (4000 + Math.random() * 100000);
            final byte[] arr = new byte[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = 'a';
            }
            return arr;
        }

        public E(final int i) {
            index = i;
            largeContent = createLargeByteArray();
        }

        public Integer getIndex() {
            return index;
        }

        public byte[] getLargeContent() {
            return largeContent;
        }
    }

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
    @Category(Slow.class)
    public void testWithManyElementsInCollection() throws Exception {
        Query<E> query = getDs().createQuery(E.class);
        final long countAll = query.countAll();
        query = getDs().createQuery(E.class);
        final List<E> list = query.asList();
        Assert.assertEquals(documentsNb, countAll);
        Assert.assertEquals(documentsNb, list.size());
    }
}