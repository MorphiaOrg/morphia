package org.mongodb.morphia.query;


import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * @author scotthernandez
 */
public class QueryInTest extends TestBase {
    private static final Logger LOG = getLogger(QueryInTest.class);

    @Test
    public void testAddEmpty() {
        Query<Data> query = getDs().createQuery(Data.class);
        List<ObjectId> memberships = new ArrayList<ObjectId>();

        query.or(
            query.criteria("id").hasAnyOf(memberships),
            query.criteria("otherIds").hasAnyOf(memberships)
        );

        List<Data> dataList = query.asList();

        Assert.assertEquals(0, dataList.size());
    }

    @Test
    public void testIdOnly() {
        ReferencedEntity b = new ReferencedEntity();
        b.setId(new ObjectId("111111111111111111111111"));
        getDs().save(b);

        HasIdOnly has = new HasIdOnly();
        has.list = new ArrayList<ReferencedEntity>();
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

    @Test
    public void testInIdList() throws Exception {
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
    public void testInQueryByKey() throws Exception {
        checkMinServerVersion(2.5);
        final HasRef hr = new HasRef();
        List<Key<ReferencedEntity>> refs = new ArrayList<Key<ReferencedEntity>>();
        for (int x = 0; x < 10; x++) {
            final ReferencedEntity re = new ReferencedEntity("" + x);
            getDs().save(re);
            refs.add(new Key<ReferencedEntity>(ReferencedEntity.class,
                                               getMorphia().getMapper().getCollectionName(ReferencedEntity.class),
                                               re.getId()));
        }
        hr.ref = refs.get(0);

        getDs().save(hr);

        Query<HasRef> query = getDs().createQuery(HasRef.class).field("ref").in(refs);
        try {
            Assert.assertEquals(1, query.asList().size());
        } catch (MongoException e) {
            LOG.debug("query = " + query);
            throw e;
        }
    }

    @Test
    public void testMapping() throws Exception {
        getMorphia().map(HasRefs.class);
        getMorphia().map(ReferencedEntity.class);
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

    @Entity("data")
    private static class Data {
        private ObjectId id;
        private Set<ObjectId> otherIds;

        public Data() {
            otherIds = new HashSet<ObjectId>();
        }
    }

    @Entity
    private static class HasRef implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private Key<ReferencedEntity> ref;
    }

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

    @Entity("docs")
    private static class Doc {
        @Id
        private long id = 4;

    }
}
