package dev.morphia.test.mapping.validation.fieldrules;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Reference;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class MapKeyTypeConstraintTest extends TestBase {

    @Test
    public void testInvalidKeyType() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            getMapper().map(MapWithWrongKeyType3.class);
        });
    }

    @Test
    @Tag("references")
    public void testInvalidReferenceType() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            getMapper().map(MapWithWrongKeyType2.class);
        });
    }

    private static class MapWithWrongKeyType2 extends TestEntity {
        @Reference
        private final Map<Integer, Integer> shouldBeOk = new HashMap<>();

    }

    private static class MapWithWrongKeyType3 extends TestEntity {
        private final Map<BigDecimal, Integer> shouldBeOk = new HashMap<>();
    }
}
