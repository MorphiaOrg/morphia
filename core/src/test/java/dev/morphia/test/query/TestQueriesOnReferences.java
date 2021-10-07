package dev.morphia.test.query;


import dev.morphia.Key;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.TestBase;
import dev.morphia.test.query.TestLegacyQuery.ContainsPic;
import dev.morphia.test.query.TestLegacyQuery.Pic;
import dev.morphia.test.query.TestLegacyQuery.PicWithObjectId;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;
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

        getDs().ensureIndexes();

        var e1_input = new Entity1();
        getDs().save(e1_input);

        var e2_input = new Entity2();
        e2_input.setReference(e1_input);
        getDs().save(e2_input);

        Runnable test = () -> {
            getDs().getMapper().map(Entity1.class, Entity2.class);

            var e1 = getDs().find(Entity1.class).first();
            var e2 = getDs().find(Entity2.class).filter(Filters.eq("reference", e1)).first();
            var e2_i = getDs().find(Entity2.class).filter(Filters.eq("reference", e1.getId())).first();

            assertNotNull(e1, "e1");
            assertNotNull(e2, "e2");
            assertNotNull(e2_i, "e2_1");
            assertEquals(e2.getId(), e2_i.getId());
        };

        test.run();

        withOptions(MapperOptions.DEFAULT, test);
    }

    @Test
    public void testKeyExists() {
        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        cpk.setPic(p);
        getDs().save(p);
        getDs().save(cpk);

        assertNotNull(getDs().find(ContainsPic.class)
                             .filter(exists("pic")).iterator(new FindOptions()
                .projection().include("pic")
                .limit(1))
                             .tryNext());
        assertNull(getDs().find(ContainsPic.class)
                          .filter(exists("pic").not()).iterator(new FindOptions()
                .projection().include("pic")
                .limit(1))
                          .tryNext());

        assertNotNull(getDs().find(ContainsPic.class).filter(Filters.eq("pic", p)).first());
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
        assertNotNull(query.filter(eq("lazyPic", p)).iterator(new FindOptions().limit(1))
                           .tryNext());

        query = getDs().find(ContainsPic.class);
        assertNotNull(query.filter(eq("lazyObjectIdPic", withObjectId)).iterator(new FindOptions().limit(1))
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
        assertNotNull(object);

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

        assertEquals(containsPic.getId(), cpk.getId(), getDs().getLoggedQuery(options));

        containsPic = query.iterator(new FindOptions().limit(1))
                           .tryNext();
        assertEquals(cpk.getId(), containsPic.getId());
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

