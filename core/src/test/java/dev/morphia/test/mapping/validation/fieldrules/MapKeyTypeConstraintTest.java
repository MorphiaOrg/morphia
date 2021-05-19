package dev.morphia.test.mapping.validation.fieldrules;


import dev.morphia.annotations.Reference;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MapKeyTypeConstraintTest extends TestBase {

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testInvalidKeyType() {
        getMapper().map(MapWithWrongKeyType3.class);
    }

    @Test(groups = "references", expectedExceptions = ConstraintViolationException.class)
    public void testInvalidReferenceType() {
        getMapper().map(MapWithWrongKeyType2.class);
    }

    private static class MapWithWrongKeyType2 extends TestEntity {
        @Reference
        private final Map<Integer, Integer> shouldBeOk = new HashMap<>();

    }

    private static class MapWithWrongKeyType3 extends TestEntity {
        private final Map<BigDecimal, Integer> shouldBeOk = new HashMap<>();
    }
}
