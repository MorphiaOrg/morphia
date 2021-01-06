package dev.morphia;


import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.legacy.TestLegacyQuery.ContainsPic;
import dev.morphia.query.legacy.TestLegacyQuery.Pic;
import dev.morphia.query.legacy.TestLegacyQuery.PicWithObjectId;
import org.junit.Assert;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;


public class TestQueriesOnReferences extends TestBase {
    @Test
    public void testKeyExists() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        Assert.assertNotNull(getDs().find(ContainsPic.class)
                                    .filter(exists("pic")).iterator(new FindOptions()
                                                                        .projection().include("pic")
                                                                        .limit(1))
                                    .tryNext());
        Assert.assertNull(getDs().find(ContainsPic.class)
                                 .filter(exists("pic").not()).iterator(new FindOptions()
                                                                           .projection().include("pic")
                                                                           .limit(1))
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

        getDs().find(ContainsPic.class).iterator().toList();
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
        Assert.assertNotNull(query.filter(eq("lazyPic", p)).iterator(new FindOptions().limit(1))
                                  .tryNext());

        query = getDs().find(ContainsPic.class);
        Assert.assertNotNull(query.filter(eq("lazyObjectIdPic", withObjectId)).iterator(new FindOptions().limit(1))
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
        final ContainsPic object = query.filter(eq("pic", p)).iterator(new FindOptions().limit(1))
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
                                          .filter(eq("pic", new Key<>(Pic.class, "pic", p.getId())));
        FindOptions options = new FindOptions()
                                  .logQuery()
                                  .limit(1);
        ContainsPic containsPic = query.iterator(options)
                                       .tryNext();

        Assert.assertEquals(getDs().getLoggedQuery(options), cpk.getId(), containsPic.getId());

        containsPic = query.iterator(new FindOptions().limit(1))
                           .tryNext();
        Assert.assertEquals(cpk.getId(), containsPic.getId());
    }
}

