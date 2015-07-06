package org.mongodb.morphia.optimisticlocks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.ConcurrentModificationException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class VersionTest extends TestBase {


    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModDetection() throws Exception {
        getMorphia().map(ALongPrimitive.class);

        final ALongPrimitive a = new ALongPrimitive();
        Assert.assertEquals(0, a.hubba);
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
        Assert.assertEquals(0, a.hubba);
        getDs().save(a);
        Assert.assertTrue(a.hubba > 0);
        final long version1 = a.hubba;

        getDs().save(a);
        Assert.assertTrue(a.hubba > 0);
        final long version2 = a.hubba;

        Assert.assertFalse(version1 == version2);
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
        private long hubba;

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

}
