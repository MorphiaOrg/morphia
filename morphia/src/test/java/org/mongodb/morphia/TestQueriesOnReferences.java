package org.mongodb.morphia;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.TestQuery.ContainsPic;
import org.mongodb.morphia.query.TestQuery.Pic;
import org.mongodb.morphia.query.TestQuery.PicWithObjectId;


public class TestQueriesOnReferences extends TestBase {
    @Test
    public void testKeyExists() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        Assert.assertNotNull(getDs().createQuery(ContainsPic.class)
                                    .field("pic").exists()
                                    .project("pic", true).get());
        Assert.assertNull(getDs().createQuery(ContainsPic.class)
                                 .field("pic").doesNotExist()
                                 .project("pic", true).get());
    }

    @Test(expected = MappingException.class)
    public void testMissingReferences() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        getDs().delete(p);

        getDs().createQuery(ContainsPic.class).asList();
    }

    @Test
    public void testQueryOverLazyReference() throws Exception {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        final PicWithObjectId withObjectId = new PicWithObjectId();
        getDs().save(withObjectId);
        cpk.setLazyPic(p);
        cpk.setLazyObjectIdPic(withObjectId);
        getDs().save(cpk);

        Query<ContainsPic> query = getDs().createQuery(ContainsPic.class);
        Assert.assertNotNull(query.field("lazyPic")
                                  .equal(p)
                                  .get());

        query = getDs().createQuery(ContainsPic.class);
        Assert.assertNotNull(query.field("lazyObjectIdPic")
                                  .equal(withObjectId)
                                  .get());
    }

    @Test
    public void testQueryOverReference() throws Exception {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        cpk.setPic(p);
        getDs().save(cpk);

        final Query<ContainsPic> query = getDs().createQuery(ContainsPic.class);
        final ContainsPic object = query.field("pic")
                                        .equal(p)
                                        .get();
        Assert.assertNotNull(object);

    }

    @Test
    public void testWithKeyQuery() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        ContainsPic containsPic = getDs().createQuery(ContainsPic.class)
                                         .field("pic").equal(new Key<Pic>(Pic.class, "Pic", p.getId()))
                                         .get();
        Assert.assertEquals(cpk.getId(), containsPic.getId());

        containsPic = getDs().createQuery(ContainsPic.class).field("pic").equal(new Key<Pic>(Pic.class, "Pic", p.getId())).get();
        Assert.assertEquals(cpk.getId(), containsPic.getId());
    }
}

