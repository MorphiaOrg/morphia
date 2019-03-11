package dev.morphia.converters;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NastyEnumTest extends TestBase {
    @Test
    public void testNastyEnumPersistence() throws Exception {
        NastyEnumEntity n = new NastyEnumEntity();
        getDs().save(n);
        n = getDs().get(n);
        Assert.assertSame(NastyEnum.A, n.e1);
        Assert.assertSame(NastyEnum.B, n.e2);
        Assert.assertNull(n.e3);
    }

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
        private final NastyEnum e1 = NastyEnum.A;
        private final NastyEnum e2 = NastyEnum.B;
        private NastyEnum e3;
    }
}
