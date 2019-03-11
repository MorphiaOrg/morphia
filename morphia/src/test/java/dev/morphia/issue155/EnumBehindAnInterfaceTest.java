package dev.morphia.issue155;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.testutil.TestEntity;


/**
 * @author josephpachod
 */
public class EnumBehindAnInterfaceTest extends TestBase {
    @Test
    @Ignore("does not work since the EnumConverter stores as a single string value -- no type info")
    public void testEnumBehindAnInterfacePersistence() throws Exception {
        getMorphia().map(ContainerEntity.class);
        ContainerEntity n = new ContainerEntity();
        getDs().save(n);
        n = getDs().get(n);
        Assert.assertSame(EnumBehindAnInterface.A, n.foo);
    }

    private static class ContainerEntity extends TestEntity {
        private final Bar foo = EnumBehindAnInterface.A;
    }
}
