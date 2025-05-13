package dev.morphia.test.query;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TestBase;
import dev.morphia.test.query.TestQuery.ContainsPic;
import dev.morphia.test.query.TestQuery.Pic;
import dev.morphia.test.query.TestQuery.PicWithObjectId;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class TestQueriesOnReferences extends TestBase {
    @Test
    public void testFindByReference() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(List.of(p, cpk));

        assertNotNull(getDs().find(ContainsPic.class).filter(Filters.eq("pic", p)).first());
    }

    @Test
    public void testFindByReference2() {
        getDs().getMapper().map(Entity1.class, Entity2.class);

        getDs().applyIndexes();

        var e1_input = new Entity1();
        getDs().save(e1_input);

        var e2_input = new Entity2();
        e2_input.setReference(e1_input);
        getDs().save(e2_input);

        var e1 = getDs().find(Entity1.class).first();
        var e2 = getDs().find(Entity2.class).filter(Filters.eq("reference", e1)).first();
        var e2_i = getDs().find(Entity2.class).filter(Filters.eq("reference", e1.getId())).first();

        assertNotNull(e1, "e1");
        assertNotNull(e2, "e2");
        assertNotNull(e2_i, "e2_1");
        assertEquals(e2.getId(), e2_i.getId());
    }

    @Test
    public void testKeyExists() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        assertNotNull(getDs().find(ContainsPic.class,
                new FindOptions()
                        .projection()
                        .include("pic")
                        .limit(1))
                .filter(exists("pic"))
                .iterator()
                .tryNext());
        assertNull(getDs().find(ContainsPic.class,
                new FindOptions()
                        .projection()
                        .include("pic")
                        .limit(1))
                .filter(exists("pic").not())
                .iterator()
                .tryNext());
    }

    @Test
    public void testMatchOnAReference() {
        getDs().getMapper().map(Entity1.class, Entity2.class);

        getDs().applyIndexes();

        var e1_input = new Entity1();
        getDs().save(e1_input);

        var e2_input = new Entity2();
        e2_input.setReference(e1_input);
        getDs().save(e2_input);

        var e1 = getDs().find(Entity1.class).first();
        var e2 = getDs().aggregate(Entity2.class)
                .pipeline(match(Filters.eq("reference", e1)))
                .iterator()
                .tryNext();
        var e2_i = getDs().aggregate(Entity2.class)
                .pipeline(match(Filters.eq("reference", e1.getId())))
                .iterator()
                .tryNext();

        assertNotNull(e1, "e1");
        assertNotNull(e2, "e2");
        assertNotNull(e2_i, "e2_1");
        assertEquals(e2.getId(), e2_i.getId());
    }

    @Test(expectedExceptions = ReferenceException.class)
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
        assertNotNull(query.filter(eq("lazyPic", p)).iterator()
                .tryNext());

        query = getDs().find(ContainsPic.class);
        assertNotNull(query.filter(eq("lazyObjectIdPic", withObjectId)).iterator()
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
        final ContainsPic object = query.filter(eq("pic", p)).iterator()
                .tryNext();
        assertNotNull(object);

    }

    @Entity
    private static class Entity1 {

        @Id
        private String _id;

        @IdGetter
        public String getId() {
            return _id;
        }
    }

    @Entity
    private static class Entity2 {
        @Reference(idOnly = true)
        private Entity1 reference;

        @Id
        private String _id;

        @IdGetter
        public String getId() {
            return _id;
        }

        public Entity1 getReference() {
            return reference;
        }

        public void setReference(Entity1 reference) {
            this.reference = reference;
        }
    }
}
