package org.mongodb.morphia.utils;


import org.junit.Assert;
import org.junit.Test;


public class FieldNameTest {

    private String foo;
    private String bar;

    @Test(expected = FieldName.FieldNameNotFoundException.class)
    public void testFieldNameOf() throws Exception {
        Assert.assertEquals("foo", FieldName.of("foo"));
        Assert.assertEquals("bar", FieldName.of("bar"));
        Assert.assertEquals("x", FieldName.of(E2.class, "x"));
        Assert.assertEquals("y", FieldName.of(E2.class, "y"));
        FieldName.of("buh");
    }
}

class E1 {
    private final int x = 0;
}

class E2 extends E1 {
    private final int y = 0;
}
