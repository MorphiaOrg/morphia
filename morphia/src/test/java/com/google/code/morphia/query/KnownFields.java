package com.google.code.morphia.query;


import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;


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
