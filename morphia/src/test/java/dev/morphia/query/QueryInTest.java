package dev.morphia.query;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.testutil.TestEntity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.in;
import static java.util.Collections.singletonList;


public class QueryInTest extends TestBase {
    @Test
    @Category(Reference.class)
    public void testIdOnly() {
        ReferencedEntity b = new ReferencedEntity();
        b.setId(new ObjectId("111111111111111111111111"));
        getDs().save(b);

        HasIdOnly has = new HasIdOnly();
        has.list = new ArrayList<>();
        has.list.add(b);
        has.entity = b;
        getDs().save(has);

        Query<HasIdOnly> q = getDs().find(HasIdOnly.class);
        q.filter(in("list", singletonList(b)));
        Assert.assertEquals(1, q.count());

        q = getDs().find(HasIdOnly.class);
        q.filter(eq("entity", b.getId()));
        Assert.assertEquals(1, q.count());
    }

    @Test
    public void testInIdList() {
        final Doc doc = new Doc();
        doc.id = 1;
        getDs().save(doc);

        // this works
        getDs().find(Doc.class).filter(eq("_id", 1)).execute();

        final List<Long> idList = new ArrayList<>();
        idList.add(1L);
        // this causes an NPE
        getDs().find(Doc.class).filter(in("_id", idList)).execute();

    }

    @Test
    @Category(Reference.class)
    public void testInQuery() {
        final HasRefs hr = new HasRefs();
        for (int x = 0; x < 10; x++) {
            final ReferencedEntity re = new ReferencedEntity("" + x);
            hr.refs.add(re);
        }
        getDs().save(hr.refs);
        getDs().save(hr);

        Query<HasRefs> query = getDs().find(HasRefs.class).filter(in("refs", hr.refs.subList(1, 3)));
        Assert.assertEquals(1, query.count());
    }

    @Test
    @Category(Reference.class)
    public void testInQueryByKey() {
        final HasRef hr = new HasRef();
        List<ReferencedEntity> refs = new ArrayList<>();
        for (int x = 0; x < 10; x++) {
            refs.add(getDs().save(new ReferencedEntity("" + x)));
        }
        hr.ref = refs.get(0);

        getDs().save(hr);

        Assert.assertEquals(1, getDs().find(HasRef.class).filter(in("ref", refs)).count());
    }

    @Test
    public void testMapping() {
        getMapper().map(HasRefs.class);
        getMapper().map(ReferencedEntity.class);
    }

    @Test
    public void testReferenceDoesNotExist() {
        final HasRefs hr = new HasRefs();
        getDs().save(hr);

        final Query<HasRefs> q = getDs().find(HasRefs.class);
        q.filter(exists("refs").not());
        Assert.assertEquals(1, q.count());
    }

    @Entity
    private static class HasRef implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private ReferencedEntity ref;
    }

    @Entity
    private static class HasRefs implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private List<ReferencedEntity> refs = new ArrayList<>();
    }

    @Entity
    private static class ReferencedEntity extends TestEntity {
        private String foo;

        ReferencedEntity() {
        }

        ReferencedEntity(final String s) {
            foo = s;
        }
    }

    @Entity(value = "as", useDiscriminator = false)
    private static class HasIdOnly {
        @Id
        private ObjectId id;
        private String name;
        @Reference(idOnly = true)
        private List<ReferencedEntity> list;
        @Reference(idOnly = true)
        private ReferencedEntity entity;
    }

    @Entity("docs")
    private static class Doc {
        @Id
        private long id = 4;

    }
}
