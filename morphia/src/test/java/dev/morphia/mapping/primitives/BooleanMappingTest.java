package dev.morphia.mapping.primitives;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BooleanMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMorphia().map(Booleans.class);
        final Booleans ent = new Booleans();
        ent.booleans.add(new Boolean[]{Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE});
        ent.list.addAll(Arrays.asList(Boolean.TRUE, Boolean.TRUE));

        ent.booleanPrimitives.add(new boolean[]{true, true, false});
        ent.singlePrimitive = false;
        ent.singleWrapper = true;
        ent.primitiveArray = new boolean[]{false, true};
        ent.wrapperArray = new Boolean[]{Boolean.FALSE, Boolean.FALSE, Boolean.TRUE};
        ent.nestedPrimitiveArray = new boolean[][]{{false, false}, {false, true}};
        ent.nestedWrapperArray = new Boolean[][]{{Boolean.FALSE, Boolean.TRUE, Boolean.FALSE},
                                                 {Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}};
        getDs().save(ent);

        final Booleans loaded = getDs().get(ent);

        Assert.assertNotNull(loaded.id);

        Assert.assertArrayEquals(ent.booleans.get(0), loaded.booleans.get(0));
        Assert.assertArrayEquals(ent.list.toArray(new Boolean[0]), loaded.list.toArray(new Boolean[0]));
        compare("booleanPrimitives", ent.booleanPrimitives.get(0), loaded.booleanPrimitives.get(0));

        Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive);
        Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper);

        compare("primitiveArray", ent.primitiveArray, loaded.primitiveArray);
        Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
        compare("nestedPrimitiveArray", ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assert.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    private void compare(final String property, final boolean[] expected, final boolean[] received) {
        Assert.assertEquals(String.format("%s lengths should match", property), expected.length, received.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(String.format("%s[%s] should match", property, i), expected[i], received[i]);
        }
    }

    private void compare(final String property, final boolean[][] expected, final boolean[][] received) {
        Assert.assertEquals(String.format("%s lengths should match", property), expected.length, received.length);
        for (int i = 0; i < expected.length; i++) {
            compare(property + "[" + i + "]", expected[i], received[i]);
        }
    }

    private static class Booleans {
        private final List<Boolean[]> booleans = new ArrayList<Boolean[]>();
        private final List<boolean[]> booleanPrimitives = new ArrayList<boolean[]>();
        private final List<Boolean> list = new ArrayList<Boolean>();
        @Id
        private ObjectId id;
        private boolean singlePrimitive;
        private Boolean singleWrapper;
        private boolean[] primitiveArray;
        private Boolean[] wrapperArray;
        private boolean[][] nestedPrimitiveArray;
        private Boolean[][] nestedWrapperArray;
    }
}
