package org.mongodb.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;


public class KnownFields extends TestBase {

    private static class A {
        String foo;
        String bar;
    }

    @Test
    public void testKnownFields() {
        Assert.assertNotNull(ds.createQuery(A.class).retrieveKnownFields());
    }
}
