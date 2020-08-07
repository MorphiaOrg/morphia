package dev.morphia.optimisticlocks;


import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
import dev.morphia.testutil.TestEntity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class VersionTest extends TestBase {


    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModDetection() throws Exception {
        getMorphia().map(VersionedType.class);

        final VersionedType a = new VersionedType();
        Assert.assertEquals(0, a.version);
        getDs().save(a);

        getDs().save(getDs().get(a));

        getDs().save(a);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModDetectionLong() throws Exception {
        final ALong a = new ALong();
        Assert.assertEquals(null, a.v);
        getDs().save(a);

        getDs().save(getDs().get(a));

        getDs().save(a);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModDetectionLongWithMerge() throws Exception {
        final ALong a = new ALong();
        Assert.assertEquals(null, a.v);
        getDs().save(a);

        a.text = " foosdfds ";
        final ALong a2 = getDs().get(a);
        getDs().save(a2);

        getDs().merge(a);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidVersionUse() throws Exception {
        getMorphia().map(InvalidVersionUse.class);
    }

    @Test
    public void testVersionFieldNameContribution() throws Exception {
        final MappedField mappedFieldByJavaField = getMorphia().getMapper().getMappedClass(ALong.class).getMappedFieldByJavaField("v");
        Assert.assertEquals("versionNameContributedByAnnotation", mappedFieldByJavaField.getNameToStore());
    }

    @Test
    public void testVersionInHashcode() throws Exception {
        getMorphia().mapPackage("com.example");

        final VersionInHashcode model = new VersionInHashcode();
        model.data = "whatever";
        getDs().save(model);
        Assert.assertNotNull(model.version);
    }

    @Test
    public void testMultiSaves() {
        getMorphia().map(VersionedType.class);
        List<VersionedType> initial = asList(new VersionedType(), new VersionedType());

        getDs().save(initial);

        Query<VersionedType> query = getDs().find(VersionedType.class);
        getDs().save(query.asList());

        List<VersionedType> loaded = query.asList();

        for (int i = 0, loadedSize = loaded.size(); i < loadedSize; i++) {
            final VersionedType type = loaded.get(i);
            assertEquals(initial.get(i).id, type.id);
            assertEquals(initial.get(i).version + 1, type.version);
        }

        Assert.assertThrows(ConcurrentModificationException.class, () -> {
            getDs().save(initial);
        });
    }

    @Test
    public void testVersions() throws Exception {
        final VersionedType a = new VersionedType();
        Assert.assertEquals(0, a.version);
        getDs().save(a);
        Assert.assertTrue(a.version > 0);
        final long version1 = a.version;

        getDs().save(a);
        Assert.assertTrue(a.version > 0);
        final long version2 = a.version;

        Assert.assertFalse(version1 == version2);
    }

    @Test
    public void testVersionsWithFindAndModify() {
        final VersionedType initial = new VersionedType();
        Datastore ds = getDs();
        ds.save(initial);

        Query<VersionedType> query = ds.find(VersionedType.class)
                                       .field("id").equal(initial.getId());
        UpdateOperations<VersionedType> update = ds.createUpdateOperations(VersionedType.class)
                                                   .set("text", "some new value");
        VersionedType postUpdate = ds.findAndModify(query, update);

        Assert.assertEquals(initial.version + 1, postUpdate.version);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testManuallyIdentifiedConcurrentModification() {
        final String id = UUID.randomUUID().toString();

        final ManuallyIdentifiedEntity entity1 = new ManuallyIdentifiedEntity();
        final ManuallyIdentifiedEntity entity2 = new ManuallyIdentifiedEntity();

        entity1.setId(id);
        entity2.setId(id);

        getDs().save(entity1);
        getDs().save(entity2);
    }

    @Test
    public void testVersionsWithUpdate() {
        final VersionedType initial = new VersionedType();
        Datastore ds = getDs();
        ds.save(initial);

        Query<VersionedType> query = ds.find(VersionedType.class)
                                       .field("id").equal(initial.getId());
        UpdateOperations<VersionedType> update = ds.createUpdateOperations(VersionedType.class)
                                                   .set("text", "some new value");
        UpdateResults results = ds.update(query, update);
        assertEquals(1, results.getUpdatedCount());
        VersionedType postUpdate = ds.get(VersionedType.class, initial.getId());

        Assert.assertEquals(initial.version + 1, postUpdate.version);
    }

    @Entity
    public static class VersionInHashcode {
        @Id
        private ObjectId id;
        @Version
        private Long version;

        private String data;

        @Override
        public int hashCode() {
            final int dataHashCode = (data == null) ? 0 : data.hashCode();
            final int versionHashCode = (version == null) ? 0 : version.hashCode();
            return dataHashCode + versionHashCode;
        }
    }

    public static class VersionedType extends TestEntity {
        @Version
        private long version;
        private String text;
    }

    public static class ALong extends TestEntity {
        @Version("versionNameContributedByAnnotation")
        private Long v;

        private String text;
    }

    @Entity
    static class InvalidVersionUse {
        @Id
        private String id;
        @Version
        private long version1;
        @Version
        private long version2;

    }

    @Entity
    public static class ManuallyIdentifiedEntity {
        @Id
        private String id;
        @Version
        private Long version;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(final Long version) {
            this.version = version;
        }
    }
}
