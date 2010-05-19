package com.google.code.morphia.issue49;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestReferenceArray extends TestBase
{

    @Test
    public final void testArrayPersistence()
    {
        A a = new A();
        B b1 = new B();
        B b2 = new B();

        a.bs[0] = b1;;
        a.bs[1] = b2;;

        ds.save(b2, b1, a);

        a = ds.get(a);
    }

   
    public static class A extends AbstractMongoEntity
    {
        @Reference
        B[] bs = new B[2];
    }

    public static class B extends AbstractMongoEntity
    {
        String foo;
    }

}
