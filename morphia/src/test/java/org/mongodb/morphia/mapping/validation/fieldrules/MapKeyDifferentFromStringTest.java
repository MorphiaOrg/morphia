package org.mongodb.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapKeyDifferentFromStringTest extends TestBase {

    @Test
    public void testCheck() {
        getMorphia().map(MapWithWrongKeyType1.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidKeyType() {
        getMorphia().map(MapWithWrongKeyType3.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidReferenceType() {
        getMorphia().map(MapWithWrongKeyType2.class);
    }

    public static class MapWithWrongKeyType1 extends TestEntity {
        @Serialized
        private Map<Integer, Integer> shouldBeOk = new HashMap<Integer, Integer>();

    }

    public static class MapWithWrongKeyType2 extends TestEntity {
        @Reference
        private Map<Integer, Integer> shouldBeOk = new HashMap<Integer, Integer>();

    }

    public static class MapWithWrongKeyType3 extends TestEntity {
        @Embedded
        private Map<BigDecimal, Integer> shouldBeOk = new HashMap<BigDecimal, Integer>();

    }

}
