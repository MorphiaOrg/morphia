package com.google.code.morphia.query;


import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.testutil.TestEntity;


/**
 * @author scotthernandez
 */
public class QueryInForReferencedList extends TestBase {

    @Entity
    private static class HasRefs {
        private static final long serialVersionUID = 1L;

        @Id
        ObjectId id = new ObjectId();
        @Reference
        final List<ReferencedEntity> refs = new ArrayList<ReferencedEntity>();
    }

    @Entity
    private static class ReferencedEntity extends TestEntity {
        private static final long serialVersionUID = 1L;
        String foo;

        public ReferencedEntity(final String s) {
            foo = s;
        }

        public ReferencedEntity() {
        }

    }

    @Entity("docs")
    private static class Doc {
        @Id
        public long id = 4;
    }

    @Test
    public void testMapping() throws Exception {

        morphia.map(HasRefs.class);
        morphia.map(ReferencedEntity.class);
    }

    @Test
    public void testInQuery() throws Exception {
        final HasRefs hr = new HasRefs();
        for (int x = 0; x < 10; x++) {
            final ReferencedEntity re = new ReferencedEntity("" + x);
            hr.refs.add(re);
        }
        ds.save(hr.refs);
        ds.save(hr);

        final List<HasRefs> res = ds.createQuery(HasRefs.class).field("refs").in(hr.refs.subList(1, 3)).asList();
        Assert.assertEquals(1, res.size());
    }

    @Test
    public void testInQuery2() throws Exception {
        final Doc doc = new Doc();
        doc.id = 1;
        ds.save(doc);

        // this works
        ds.find(Doc.class).field("_id").equal(1).asList();

        final List<Long> idList = new ArrayList<Long>();
        idList.add(1L);
        // this causes an NPE
        ds.find(Doc.class).field("_id").in(idList).asList();

    }

    @Test
    public void testReferenceDoesNotExist() {
        final HasRefs hr = new HasRefs();
        ds.save(hr);

        final Query<HasRefs> q = ds.createQuery(HasRefs.class);
        q.field("refs").doesNotExist();
        final List<HasRefs> found = q.asList();
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
    }
}
