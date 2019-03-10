package dev.morphia.optimisticlocks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
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

import java.util.ConcurrentModificationException;

import static org.junit.Assert.assertEquals;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class VersionTest extends TestBase {


    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModDetection() throws Exception {
        getMorphia().map(ALongPrimitive.class);

        final ALongPrimitive a = new ALongPrimitive();
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
    public void testVersions() throws Exception {
        final ALongPrimitive a = new ALongPrimitive();
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
        final ALongPrimitive initial = new ALongPrimitive();
        Datastore ds = getDs();
        ds.save(initial);

        Query<ALongPrimitive> query = ds.find(ALongPrimitive.class)
                                     .field("id").equal(initial.getId());
        UpdateOperations<ALongPrimitive> update = ds.createUpdateOperations(ALongPrimitive.class)
                                                    .set("text", "some new value");
        ALongPrimitive postUpdate = ds.findAndModify(query, update);

        Assert.assertEquals(initial.version + 1, postUpdate.version);
    }

    @Test
    public void testVersionsWithUpdate() {
        final ALongPrimitive initial = new ALongPrimitive();
        Datastore ds = getDs();
        ds.save(initial);

        Query<ALongPrimitive> query = ds.find(ALongPrimitive.class)
                                     .field("id").equal(initial.getId());
        UpdateOperations<ALongPrimitive> update = ds.createUpdateOperations(ALongPrimitive.class)
                                                    .set("text", "some new value");
        UpdateResults results = ds.update(query, update);
        assertEquals(1, results.getUpdatedCount());
        ALongPrimitive postUpdate = ds.get(ALongPrimitive.class, initial.getId());

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

    public static class ALongPrimitive extends TestEntity {

        @Version
        private long version;

        private String text;

        public long getVersion() {
            return version;
        }

        public void setVersion(final long version) {
            this.version = version;
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }
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

}
