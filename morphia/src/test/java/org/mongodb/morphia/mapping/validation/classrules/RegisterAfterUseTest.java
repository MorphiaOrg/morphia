package org.mongodb.morphia.mapping.validation.classrules;


import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class RegisterAfterUseTest extends TestBase {

  public static class Broken extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Property("foo")
    @Embedded("bar")
    List l;
  }

  @Test
  @Ignore(value = "not yet implemented")
  public void testRegisterAfterUse() throws Exception {

    // this would have failed: morphia.map(Broken.class);

    final Broken b = new Broken();
    ds.save(b); // imho must not work
    Assert.fail();

    // doe not revalidate due to being used already!
    morphia.map(Broken.class);
    Assert.fail();
  }
}
