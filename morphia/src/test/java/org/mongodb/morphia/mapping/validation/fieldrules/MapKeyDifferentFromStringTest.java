package org.mongodb.morphia.mapping.validation.fieldrules;


import java.math.BigDecimal;
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
public class MapKeyDifferentFromStringTest extends TestBase {

  public static class MapWithWrongKeyType1 extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Serialized Map<Integer, Integer> shouldBeOk = new HashMap();

  }

  public static class MapWithWrongKeyType2 extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Reference Map<Integer, Integer> shouldBeOk = new HashMap();

  }

  public static class MapWithWrongKeyType3 extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Embedded Map<BigDecimal, Integer> shouldBeOk = new HashMap();

  }

  @Test
  public void testCheck() {
    morphia.map(MapWithWrongKeyType1.class);

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        morphia.map(MapWithWrongKeyType2.class);
      }
    };

    new AssertedFailure() {
      @Override
      public void thisMustFail() {
        morphia.map(MapWithWrongKeyType3.class);
      }
    };
  }

}
