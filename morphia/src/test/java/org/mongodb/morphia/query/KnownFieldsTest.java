package org.mongodb.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;


public class KnownFieldsTest extends TestBase {

    @Test
    public void testKnownFields() {
        Assert.assertNotNull(getDs().createQuery(A.class).retrieveKnownFields());
    }

    private static class A {
        private String foo;
        private String bar;
    }
}
