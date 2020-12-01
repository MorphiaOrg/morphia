package dev.morphia.issue155;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.testutil.TestEntity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;


public class EnumBehindAnInterfaceTest extends TestBase {
    @Test
    @Ignore("does not work since the EnumConverter stores as a single string value -- no type info")
    public void testEnumBehindAnInterfacePersistence() {
        getMapper().map(ContainerEntity.class);
        ContainerEntity n = new ContainerEntity();
        getDs().save(n);
        n = getDs().find(ContainerEntity.class)
                   .filter(eq("_id", n.getId()))
                   .first();
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

    @Entity
    interface Bar {

    }
}
