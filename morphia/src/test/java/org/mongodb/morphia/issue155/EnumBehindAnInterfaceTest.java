package org.mongodb.morphia.issue155;


import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.junit.Assert;


/**
 * @author josephpachod
 */
public class EnumBehindAnInterfaceTest extends TestBase {
  @Test @Ignore("does not work since the EnumConverter stores as a single string value -- no type info")
  public void testEnumBehindAnInterfacePersistence() throws Exception {
    morphia.map(ContainerEntity.class);
    ContainerEntity n = new ContainerEntity();
    ds.save(n);
    n = ds.get(n);
    Assert.assertSame(EnumBehindAnInterface.A, n.foo);
  }
}
