package dev.morphia.issue155;


import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.testutil.TestEntity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


public class EnumBehindAnInterfaceTest extends TestBase {
    @Test
    @Ignore("does not work since the EnumConverter stores as a single string value -- no type info")
    public void testEnumBehindAnInterfacePersistence() {
        getMapper().map(ContainerEntity.class);
        ContainerEntity n = new ContainerEntity();
        getDs().save(n);
        n = getDs().get(n);
        Assert.assertSame(EnumBehindAnInterface.A, n.foo);
    }

    @Entity
    private static class ContainerEntity extends TestEntity {
        private final Bar foo = EnumBehindAnInterface.A;
    }

    enum EnumBehindAnInterface implements Bar {
        A,
        B
    }

    @Embedded
    interface Bar {

    }
}
