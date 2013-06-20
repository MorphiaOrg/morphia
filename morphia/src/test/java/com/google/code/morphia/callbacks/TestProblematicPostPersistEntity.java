package com.google.code.morphia.callbacks;


import org.junit.Test;
import com.google.code.morphia.TestBase;
import junit.framework.Assert;


public class TestProblematicPostPersistEntity extends TestBase {

  @Test
  public void testCallback() throws Exception {
    final ProblematicPostPersistEntity p = new ProblematicPostPersistEntity();
    ds.save(p);
    Assert.assertTrue(p.called);
    Assert.assertTrue(p.i.called);
  }
}
