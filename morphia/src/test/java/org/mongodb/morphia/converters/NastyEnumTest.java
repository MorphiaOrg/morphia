package org.mongodb.morphia.converters;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.testutil.TestEntity;
import org.junit.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NastyEnumTest extends TestBase {
  public enum NastyEnum {
    A {
      @Override
      public String toString() {
        return "Never use toString for other purposes than debugging";
      }
    },
    B {
      public String toString() {
        return "Never use toString for other purposes than debugging ";
      }
    }
  }

  public static class NastyEnumEntity extends TestEntity {
    private static final long serialVersionUID = 1L;
    final NastyEnum e1 = NastyEnum.A;
    final NastyEnum e2 = NastyEnum.B;
    NastyEnum e3;
  }

  @Test
  public void testNastyEnumPersistence() throws Exception {
    NastyEnumEntity n = new NastyEnumEntity();
    ds.save(n);
    n = ds.get(n);
    Assert.assertSame(NastyEnum.A, n.e1);
    Assert.assertSame(NastyEnum.B, n.e2);
    Assert.assertNull(n.e3);
  }
}
