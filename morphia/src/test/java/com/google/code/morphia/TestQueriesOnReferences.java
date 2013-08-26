package com.google.code.morphia;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.query.Query;


//@Ignore //https://github.com/mongodb/morphia/issues/62
public class TestQueriesOnReferences extends TestBase {
  @Entity
  public static class ContainsPic {
    @Id
    String id;
    @Reference
    Pic pic;
    @Reference(lazy = true)
    Pic lazyPic;
    @Reference(lazy = true)
    PicWithObjectId lazyObjectIdPic;
  }

  @Entity
  public static class Pic {
    @Id
    String id;
    String name;

    public Pic() {
      id = new ObjectId().toString();
    }
  }
  
  @Entity
  public static class PicWithObjectId {
    @Id
    ObjectId id;
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
    final PicWithObjectId withObjectId = new PicWithObjectId();
    ds.save(withObjectId);
    cpk.lazyPic = p;
    cpk.lazyObjectIdPic = withObjectId;
    ds.save(cpk);

    Query<ContainsPic> query = ds.createQuery(ContainsPic.class);
    Assert.assertNotNull(query.field("lazyPic")
      .equal(p)
      .get());

    query = ds.createQuery(ContainsPic.class);
    Assert.assertNotNull(query.field("lazyObjectIdPic")
      .equal(withObjectId)
      .get());
  }
}

