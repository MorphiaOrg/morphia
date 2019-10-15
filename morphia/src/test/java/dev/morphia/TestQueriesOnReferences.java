package dev.morphia;


import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.TestQuery.ContainsPic;
import dev.morphia.query.TestQuery.Pic;
import dev.morphia.query.TestQuery.PicWithObjectId;
import org.junit.Assert;
import org.junit.Test;


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
                                    .project("pic", true).execute(new FindOptions().limit(1))
                                    .tryNext());
        Assert.assertNull(getDs().find(ContainsPic.class)
                                 .field("pic").doesNotExist()
                                 .project("pic", true).execute(new FindOptions().limit(1))
                                 .tryNext());
    }

    @Test(expected = ReferenceException.class)
    public void testMissingReferences() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        getDs().delete(p);

        getDs().find(ContainsPic.class).execute().toList();
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
                                  .execute(new FindOptions().limit(1))
                                  .tryNext());

        query = getDs().find(ContainsPic.class);
        Assert.assertNotNull(query.field("lazyObjectIdPic")
                                  .equal(withObjectId)
                                  .execute(new FindOptions().limit(1))
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
                                        .execute(new FindOptions().limit(1))
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

        Query<ContainsPic> query = getDs().find(ContainsPic.class)
                                          .field("pic").equal(new Key<>(Pic.class, "Pic", p.getId()));
        ContainsPic containsPic = query.execute(new FindOptions()
                                                    .logQuery()
                                                    .limit(1))
                                       .tryNext();

        Assert.assertEquals(query.getLoggedQuery(), cpk.getId(), containsPic.getId());

        containsPic = query.execute(new FindOptions().limit(1))
                           .tryNext();
        Assert.assertEquals(cpk.getId(), containsPic.getId());
    }
}

