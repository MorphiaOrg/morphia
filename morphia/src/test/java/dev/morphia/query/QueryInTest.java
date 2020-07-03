package dev.morphia.query;


import com.mongodb.MongoException;
import dev.morphia.Key;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.testutil.TestEntity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * @author scotthernandez
 */
public class QueryInTest extends TestBase {
    private static final Logger LOG = getLogger(QueryInTest.class);

    @Test
    public void testAddEmpty() {
        Query<Data> query = getDs().find(Data.class);
        List<ObjectId> memberships = new ArrayList<ObjectId>();

        query.or(
            query.criteria("id").hasAnyOf(memberships),
            query.criteria("otherIds").hasAnyOf(memberships)
        );

        Assert.assertFalse(query.find().hasNext());
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

        Query<HasIdOnly> q = getDs().find(HasIdOnly.class);
        q.criteria("list").in(singletonList(b));
        Assert.assertEquals(1, q.count());

        q = getDs().find(HasIdOnly.class);
        q.criteria("entity").equal(b.getId());
        Assert.assertEquals(1, q.count());
    }

    @Test
    public void testInIdList() {
        final Doc doc = new Doc();
        doc.id = 1;
        getDs().save(doc);

        // this works
        getDs().find(Doc.class).field("_id").equal(1).find();

        final List<Long> idList = new ArrayList<Long>();
        idList.add(1L);
        // this causes an NPE
        getDs().find(Doc.class).field("_id").in(idList).find();

    }

    @Test
    public void testInQuery() {
        assumeMinServerVersion(2.5);
        final HasRefs hr = new HasRefs();
        for (int x = 0; x < 10; x++) {
            final ReferencedEntity re = new ReferencedEntity("" + x);
            hr.refs.add(re);
        }
        getDs().save(hr.refs);
        getDs().save(hr);

        Query<HasRefs> query = getDs().find(HasRefs.class).field("refs").in(hr.refs.subList(1, 3));
        Assert.assertEquals(1, query.count());
    }

    @Test
    public void testInQueryByKey() {
        assumeMinServerVersion(2.5);
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

        Query<HasRef> query = getDs().find(HasRef.class).field("ref").in(refs);
        try {
            Assert.assertEquals(1, query.count());
        } catch (MongoException e) {
            LOG.debug("query = " + query);
            throw e;
        }
    }

    @Test
    public void testMapping() {
        getMorphia().map(HasRefs.class);
        getMorphia().map(ReferencedEntity.class);
    }

    @Test
    public void testReferenceDoesNotExist() {
        final HasRefs hr = new HasRefs();
        getDs().save(hr);

        final Query<HasRefs> q = getDs().find(HasRefs.class);
        q.field("refs").doesNotExist();
        Assert.assertEquals(1, q.count());
    }

    @Entity("data")
    private static final class Data {
        private ObjectId id;
        private final Set<ObjectId> otherIds;

        private Data() {
            otherIds = new HashSet<ObjectId>();
        }
    }

    @Entity
    private static class HasRef implements Serializable {
        @Id
        private final ObjectId id = new ObjectId();
        @Reference
        private Key<ReferencedEntity> ref;
    }

    @Entity
    private static class HasRefs implements Serializable {
        @Id
        private final ObjectId id = new ObjectId();
        @Reference
        private final List<ReferencedEntity> refs = new ArrayList<ReferencedEntity>();
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

    @Entity(value = "as", noClassnameStored = true)
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
