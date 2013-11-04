package org.mongodb.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;


public class KnownFieldsTest extends TestBase {

    private static class A {
        private String foo;
        private String bar;
    }

    @Test
    public void testKnownFields() {
        Assert.assertNotNull(getDs().createQuery(A.class).retrieveKnownFields());
    }
}
