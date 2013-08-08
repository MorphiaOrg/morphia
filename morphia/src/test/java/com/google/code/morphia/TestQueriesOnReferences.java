package com.google.code.morphia;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.query.Query;


@Ignore //https://github.com/mongodb/morphia/issues/62
public class TestQueriesOnReferences extends TestBase {
  @Entity
  public static class ContainsPic {
    @Id
    String id;
    @Reference
    Pic pic;
    @Reference(lazy = true)
    Pic lazyPic;
  }

  @Entity
  public static class Pic {
    @Id
    String id;
    String name;
  }

  @Test
  public void testQueryOverReference() throws Exception {

    final ContainsPic cpk = new ContainsPic();
    final Pic p = new Pic();
    ds.save(p);
    cpk.pic = p;
    ds.save(cpk);

    final Query<ContainsPic> query = ds.createQuery(ContainsPic.class);
    final ContainsPic object = query.field("pic")
      .equal(p)
      .get();
    Assert.assertNotNull(object);

  }

  @Test
  public void testQueryOverLazyReference() throws Exception {

    final ContainsPic cpk = new ContainsPic();
    final Pic p = new Pic();
    ds.save(p);
    cpk.lazyPic = p;
    ds.save(cpk);

    final Query<ContainsPic> query = ds.createQuery(ContainsPic.class);
    Assert.assertNotNull(query.field("lazyPic")
      .equal(p)
      .get());
  }
}

