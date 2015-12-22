package org.mongodb.morphia.mapping.lazy;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.Iterator;


public class LazyWithMissingReferentTest extends TestBase {

    @Test(expected = MappingException.class)
    public void testMissingRef() throws Exception {
        final E e = new E();
        e.e2 = new E2();

        getDs().save(e); // does not fail due to pre-initialized Ids

        getDs().createQuery(E.class).asList();
    }

    @Test(expected = LazyReferenceFetchingException.class)
    public void testMissingRefLazy() throws Exception {
        final ELazy e = new ELazy();
        e.e2 = new E2();

        getDs().save(e); // does not fail due to pre-initialized Ids
        ELazy eLazy = getDs().createQuery(ELazy.class).get();
        Assert.assertNull(eLazy.e2);
    }

    @Test(expected = Exception.class)
    public void testMissingRefLazyIgnoreMissing() throws Exception {
        final ELazyIgnoreMissing e = new ELazyIgnoreMissing();
        e.e2 = new E2();

        getDs().save(e); // does not fail due to pre-initialized Ids
        final Iterator<ELazyIgnoreMissing> i = getDs().createQuery(ELazyIgnoreMissing.class).iterator();
        final ELazyIgnoreMissing x = i.next();

        x.e2.foo();
    }

    static class E {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private E2 e2;
    }

    static class ELazy {
        @Id
        private ObjectId id = new ObjectId();
        @Reference(lazy = true)
        private E2 e2;
    }

    static class ELazyIgnoreMissing {
        @Id
        private ObjectId id = new ObjectId();
        @Reference(lazy = true, ignoreMissing = true)
        private E2 e2;
    }

    static class E2 extends TestEntity {
        @Id
        private ObjectId id = new ObjectId();
        private String foo = "bar";

        void foo() {
        }

    }
}
