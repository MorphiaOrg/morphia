package org.mongodb.morphia.mapping.validation.fieldrules;


import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class MapNotSerializableTest extends TestBase {
  public static class Map1 extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Serialized Map<Integer, String> shouldBeOk = new HashMap();

  }

  public static class Map2 extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Reference Map<Integer, E1> shouldBeOk = new HashMap();

  }

  public static class Map3 extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Embedded Map<E2, Integer> shouldBeOk = new HashMap();

  }

  public static class E1 {

  }

  public static class E2 {

  }

  @Test
  public void testCheck() {
    morphia.map(Map1.class);

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        morphia.map(Map2.class);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        morphia.map(Map3.class);
      }
    };
  }

}
