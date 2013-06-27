package com.google.code.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.testutil.TestEntity;
import com.mongodb.DBObject;
import org.junit.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class SerializedNameTest extends TestBase {
  public static class E extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Serialized("changedName") byte[] b = "foo".getBytes();

    @PreSave
    public void preSave(final DBObject o) {
      document = o.toString();
      //			System.out.println(document);
    }

    @Transient String document;
  }

  @Test
  public void testCheck() {

    final E e = new E();
    ds.save(e);

    Assert.assertTrue(e.document.contains("changedName"));
  }
}
