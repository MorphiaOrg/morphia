package dev.morphia.converters;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.query.Query;
import dev.morphia.testutil.TestEntity;

import java.util.EnumSet;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EnumSetTest extends TestBase {
    @Test
    public void testNastyEnumPersistence() throws Exception {
        NastyEnumEntity n = new NastyEnumEntity();
        getDs().save(n);
        n = getDs().get(n);

        Assert.assertNull(n.isNull);
        Assert.assertNotNull(n.empty);
        Assert.assertNotNull(n.in);
        Assert.assertNotNull(n.out);

        Assert.assertEquals(0, n.empty.size());
        Assert.assertEquals(3, n.in.size());
        Assert.assertEquals(1, n.out.size());

        Assert.assertTrue(n.in.contains(NastyEnum.B));
        Assert.assertTrue(n.in.contains(NastyEnum.C));
        Assert.assertTrue(n.in.contains(NastyEnum.D));
        Assert.assertFalse(n.in.contains(NastyEnum.A));

        Assert.assertTrue(n.out.contains(NastyEnum.A));
        Assert.assertFalse(n.out.contains(NastyEnum.B));
        Assert.assertFalse(n.out.contains(NastyEnum.C));
        Assert.assertFalse(n.out.contains(NastyEnum.D));

        Query<NastyEnumEntity> q = getDs().find(NastyEnumEntity.class).filter("in", NastyEnum.C);
        Assert.assertEquals(1, q.count());
        q = getDs().find(NastyEnumEntity.class).filter("out", NastyEnum.C);
        Assert.assertEquals(0, q.count());

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
        },
        C,
        D
    }

    public static class NastyEnumEntity extends TestEntity {
        private final EnumSet<NastyEnum> in = EnumSet.of(NastyEnum.B, NastyEnum.C, NastyEnum.D);
        private final EnumSet<NastyEnum> out = EnumSet.of(NastyEnum.A);
        private final EnumSet<NastyEnum> empty = EnumSet.noneOf(NastyEnum.class);
        private EnumSet<NastyEnum> isNull;
    }
}
