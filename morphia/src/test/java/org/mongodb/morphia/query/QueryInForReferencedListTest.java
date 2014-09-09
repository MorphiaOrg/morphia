package org.mongodb.morphia.query;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author scotthernandez
 */
public class QueryInForReferencedListTest extends TestBase {

    private String classpath;

    @Entity
    private static class HasRefs implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private List<ReferencedEntity> refs = new ArrayList<ReferencedEntity>();
    }

    @Entity
    private static class ReferencedEntity extends TestEntity {
        private String foo;

        public ReferencedEntity() {
        }

        public ReferencedEntity(final String s) {
            foo = s;
        }
    }

    @Entity("docs")
    private static class Doc {
        @Id
        private long id = 4;
    }


    @Entity(value = "as", noClassnameStored = true)
    public static class HasIdOnly {
        @Id
        private ObjectId id;
        private String name;
        @Reference(idOnly = true)
        private List<ReferencedEntity> list;
        @Reference(idOnly = true)
        private ReferencedEntity entity;
    }

    @Test
    public void testMapping() throws Exception {
        getMorphia().map(HasRefs.class);
        getMorphia().map(ReferencedEntity.class);
    }

    @Test
    public void testInQuery() throws Exception {
        checkMinServerVersion(2.5);
        final HasRefs hr = new HasRefs();
        for (int x = 0; x < 10; x++) {
            final ReferencedEntity re = new ReferencedEntity("" + x);
            hr.refs.add(re);
        }
        getDs().save(hr.refs);
        getDs().save(hr);

        Query<HasRefs> query = getDs().createQuery(HasRefs.class).field("refs").in(hr.refs.subList(1, 3));
        final List<HasRefs> res = query.asList();
        Assert.assertEquals(1, res.size());
    }

    @Test
    public void testInQuery2() throws Exception {
        final Doc doc = new Doc();
        doc.id = 1;
        getDs().save(doc);

        // this works
        getDs().find(Doc.class).field("_id").equal(1).asList();

        final List<Long> idList = new ArrayList<Long>();
        idList.add(1L);
        // this causes an NPE
        getDs().find(Doc.class).field("_id").in(idList).asList();

    }

    @Test
    public void testReferenceDoesNotExist() {
        final HasRefs hr = new HasRefs();
        getDs().save(hr);

        final Query<HasRefs> q = getDs().createQuery(HasRefs.class);
        q.field("refs").doesNotExist();
        final List<HasRefs> found = q.asList();
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
    }
    
    @Test
    public void testIdOnly() {
        ReferencedEntity b = new ReferencedEntity();
        b.setId(new ObjectId("111111111111111111111111"));
        getDs().save(b);
        
        HasIdOnly has = new HasIdOnly();
        has.list =  new ArrayList<ReferencedEntity>();
        has.list.add(b);
        has.entity = b;
        getDs().save(has);
        
        Query<HasIdOnly> q = getDs().createQuery(HasIdOnly.class);
        q.criteria("list").in(Arrays.asList(b));
        Assert.assertEquals(1, q.asList().size());
        
        q = getDs().createQuery(HasIdOnly.class);
        q.criteria("entity").equal(b.getId());
        Assert.assertEquals(1, q.asList().size());
    }
}
