package dev.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;


public class KnownFieldsTest extends TestBase {

    @Test
    public void testKnownFields() {
        Assert.assertNotNull(getDs().find(A.class).retrieveKnownFields());
    }

    private static class A {
        private String foo;
        private String bar;
    }
}
