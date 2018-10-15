package xyz.morphia;


import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.mapping.MappingException;
import xyz.morphia.query.FindOptions;
import xyz.morphia.query.Query;
import xyz.morphia.query.TestQuery.ContainsPic;
import xyz.morphia.query.TestQuery.Pic;
import xyz.morphia.query.TestQuery.PicWithObjectId;


public class TestQueriesOnReferences extends TestBase {
    @Test
    public void testKeyExists() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        Assert.assertNotNull(getDs().find(ContainsPic.class)
                                    .field("pic").exists()
                                    .project("pic", true).find(new FindOptions().limit(1))
                                    .tryNext());
        Assert.assertNull(getDs().find(ContainsPic.class)
                                 .field("pic").doesNotExist()
                                 .project("pic", true).find(new FindOptions().limit(1))
                                 .tryNext());
    }

    @Test(expected = MappingException.class)
    public void testMissingReferences() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        getDs().delete(p);

        getDs().find(ContainsPic.class).asList();
    }

    @Test
    public void testQueryOverLazyReference() {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        final PicWithObjectId withObjectId = new PicWithObjectId();
        getDs().save(withObjectId);
        cpk.setLazyPic(p);
        cpk.setLazyObjectIdPic(withObjectId);
        getDs().save(cpk);

        Query<ContainsPic> query = getDs().find(ContainsPic.class);
        Assert.assertNotNull(query.field("lazyPic")
                                  .equal(p)
                                  .find(new FindOptions().limit(1))
                                  .tryNext());

        query = getDs().find(ContainsPic.class);
        Assert.assertNotNull(query.field("lazyObjectIdPic")
                                  .equal(withObjectId)
                                  .find(new FindOptions().limit(1))
                                  .tryNext());
    }

    @Test
    public void testQueryOverReference() {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        cpk.setPic(p);
        getDs().save(cpk);

        final Query<ContainsPic> query = getDs().find(ContainsPic.class);
        final ContainsPic object = query.field("pic")
                                        .equal(p)
                                        .find(new FindOptions().limit(1))
                                        .tryNext();
        Assert.assertNotNull(object);

    }

    @Test
    public void testWithKeyQuery() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        ContainsPic containsPic = getDs().find(ContainsPic.class)
                                         .field("pic").equal(new Key<Pic>(Pic.class, "Pic", p.getId()))
                                         .find(new FindOptions().limit(1))
                                         .tryNext();
        Assert.assertEquals(cpk.getId(), containsPic.getId());

        containsPic = getDs().find(ContainsPic.class).field("pic").equal(new Key<Pic>(Pic.class, "Pic", p.getId()))
                             .find(new FindOptions().limit(1))
                             .tryNext();
        Assert.assertEquals(cpk.getId(), containsPic.getId());
    }
}

