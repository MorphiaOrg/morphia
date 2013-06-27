package com.google.code.morphia.optimisticlocks;


import java.util.ConcurrentModificationException;

import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.testutil.TestEntity;
import org.junit.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class VersionTest extends TestBase {


  public static class ALongPrimitive extends TestEntity {
    private static final long serialVersionUID = 1L;

    @Version long hubba;

    String text;
  }

  public static class ALong extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Version("versionNameContributedByAnnotation") Long v;

    String text;
  }

  @Entity
  static class InvalidVersionUse {
    @Id      String id;
    @Version long   version1;
    @Version long   version2;

  }

  @Test
  public void testInvalidVersionUse() throws Exception {
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(InvalidVersionUse.class);
      }
    };

  }

  @Test
  public void testVersions() throws Exception {
    final ALongPrimitive a = new ALongPrimitive();
    Assert.assertEquals(0, a.hubba);
    ds.save(a);
    Assert.assertTrue(a.hubba > 0);
    final long version1 = a.hubba;

    ds.save(a);
    Assert.assertTrue(a.hubba > 0);
    final long version2 = a.hubba;

    Assert.assertFalse(version1 == version2);
  }

  @Test
  public void testConcurrentModDetection() throws Exception {
    morphia.map(ALongPrimitive.class);

    final ALongPrimitive a = new ALongPrimitive();
    Assert.assertEquals(0, a.hubba);
    ds.save(a);

    ds.save(ds.get(a));


    new AssertedFailure(ConcurrentModificationException.class) {
      @Override
      public void thisMustFail() {
        ds.save(a);
      }
    };
  }

  @Test
  public void testConcurrentModDetectionLong() throws Exception {
    final ALong a = new ALong();
    Assert.assertEquals(null, a.v);
    ds.save(a);

    ds.save(ds.get(a));

    new AssertedFailure(ConcurrentModificationException.class) {
      @Override
      public void thisMustFail() {
        ds.save(a);
      }
    };
  }

  @Test
  public void testConcurrentModDetectionLongWithMerge() throws Exception {
    final ALong a = new ALong();
    Assert.assertEquals(null, a.v);
    ds.save(a);

    a.text = " foosdfds ";
    final ALong a2 = ds.get(a);
    ds.save(a2);

    new AssertedFailure(ConcurrentModificationException.class) {
      @Override
      public void thisMustFail() {
        ds.merge(a);
      }
    };
  }

  @Test
  public void testVersionFieldNameContribution() throws Exception {
    final MappedField mappedFieldByJavaField = morphia.getMapper().getMappedClass(ALong.class).getMappedFieldByJavaField("v");
    Assert.assertEquals("versionNameContributedByAnnotation", mappedFieldByJavaField.getNameToStore());
  }

}
