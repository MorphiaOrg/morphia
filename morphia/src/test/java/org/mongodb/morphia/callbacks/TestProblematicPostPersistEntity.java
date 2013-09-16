package org.mongodb.morphia.callbacks;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;


public class TestProblematicPostPersistEntity extends TestBase {

  @Test
  public void testCallback() throws Exception {
    final ProblematicPostPersistEntity p = new ProblematicPostPersistEntity();
    ds.save(p);
    Assert.assertTrue(p.called);
    Assert.assertTrue(p.i.called);
  }
}
