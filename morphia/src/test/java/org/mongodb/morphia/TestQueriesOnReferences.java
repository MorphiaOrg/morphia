package org.mongodb.morphia;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.query.Query;


public class TestQueriesOnReferences extends TestBase {
    @Test
    public void testKeyExists() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.pic = p;
        getDs().save(p);
        getDs().save(cpk);

        Assert.assertNotNull(getDs().createQuery(ContainsPic.class)
                                    .field("pic").exists()
                                    .retrievedFields(true, "pic").get());
        Assert.assertNull(getDs().createQuery(ContainsPic.class)
                                 .field("pic").doesNotExist()
                                 .retrievedFields(true, "pic").get());
    }

    @Test(expected = MappingException.class)
    public void testMissingReferences() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.pic = p;
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
        cpk.lazyPic = p;
        cpk.lazyObjectIdPic = withObjectId;
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
        cpk.pic = p;
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
        cpk.pic = p;
        getDs().save(p);
        getDs().save(cpk);

        ContainsPic containsPic = getDs().createQuery(ContainsPic.class).field("pic").equal(new Key<Pic>(Pic.class, "Pic", p.id)).get();
        Assert.assertEquals(cpk.id, containsPic.id);

        containsPic = getDs().createQuery(ContainsPic.class).field("pic").equal(new Key<Pic>(Pic.class, "Pic", p.id)).get();
        Assert.assertEquals(cpk.id, containsPic.id);
    }

    @Entity
    public static class ContainsPic {
        @Id
        private String id;
        @Reference
        private Pic pic;
        @Reference(lazy = true)
        private Pic lazyPic;
        @Reference(lazy = true)
        private PicWithObjectId lazyObjectIdPic;
    }

    @Entity
    public static class Pic {
        @Id
        private String id;
        private String name;

        public Pic() {
            id = new ObjectId().toString();
        }
    }

    @Entity
    public static class PicWithObjectId {
        @Id
        private ObjectId id;
        private String name;
    }
}

