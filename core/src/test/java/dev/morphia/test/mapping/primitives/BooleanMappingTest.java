package dev.morphia.test.mapping.primitives;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.lang.String.format;


public class BooleanMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Booleans.class);
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

        final Booleans loaded = getDs().find(Booleans.class)
                                       .filter(eq("_id", ent.id))
                                       .first();

        Assert.assertNotNull(loaded.id);

        Assert.assertEquals(ent.booleans.get(0), loaded.booleans.get(0));
        Assert.assertEquals(ent.list.toArray(new Boolean[0]), loaded.list.toArray(new Boolean[0]));
        compare("booleanPrimitives", ent.booleanPrimitives.get(0), loaded.booleanPrimitives.get(0));

        Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive);
        Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper);

        compare("primitiveArray", ent.primitiveArray, loaded.primitiveArray);
        Assert.assertEquals(ent.wrapperArray, loaded.wrapperArray);
        compare(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assert.assertEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    private void compare(String property, boolean[] expected, boolean[] received) {
        Assert.assertEquals(received.length, expected.length, format("%s lengths should match", property));
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(received[i], expected[i], format("%s[%s] should match", property, i));
        }
    }

    private void compare(boolean[][] expected, boolean[][] received) {
        Assert.assertEquals(received.length, expected.length, "nestedPrimitiveArray lengths should match");
        for (int i = 0; i < expected.length; i++) {
            compare("nestedPrimitiveArray" + "[" + i + "]", expected[i], received[i]);
        }
    }

    @Entity
    private static class Booleans {
        private final List<Boolean[]> booleans = new ArrayList<>();
        private final List<boolean[]> booleanPrimitives = new ArrayList<>();
        private final List<Boolean> list = new ArrayList<>();
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
