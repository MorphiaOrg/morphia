package dev.morphia.test.mapping.validation.classrules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.codec.pojo.ReflectiveEntityModelBuilder;
import dev.morphia.mapping.internal.FinalFieldMutation;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.mapping.validation.classrules.FinalFieldsInConstructor;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinalFieldsInConstructorTest extends TestBase {
    @Test
    public void warnsWhenAllowed() {
        List<ConstraintViolation> violations = check(FinalNotInConstructor.class, false);
        assertEquals(1, violations.size(), violations.toString());
        assertEquals(Level.WARNING, violations.get(0).getLevel());
        assertTrue(violations.get(0).render().contains("[name]"), violations.get(0).render());
    }

    @Test
    public void failsWhenDenied() {
        List<ConstraintViolation> violations = check(FinalNotInConstructor.class, true);
        assertEquals(1, violations.size(), violations.toString());
        assertEquals(Level.FATAL, violations.get(0).getLevel());
    }

    @Test
    public void lifecycleMethodsRequireNoArgConstructor() {
        List<ConstraintViolation> violations = check(LifecycleWithFinal.class, true);
        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).render().contains("lifecycle"), violations.get(0).render());
    }

    @Test
    public void constructorTakingFinals() {
        assertEquals(List.of(), check(FinalInConstructor.class, true));
        assertEquals(List.of(), check(ConstructorChoice.class, true));
    }

    @Test
    public void mappingFailsOnlyWhenDenied() {
        if (FinalFieldMutation.denied()) {
            assertThrows(ConstraintViolationException.class, () -> getMapper().map(FinalNotInConstructor.class));
        } else {
            assertDoesNotThrow(() -> getMapper().map(FinalNotInConstructor.class));
        }
    }

    @Test
    public void mixesConstructorAndFields() {
        getMapper().map(FinalInConstructor.class);
        FinalInConstructor entity = new FinalInConstructor("final");
        entity.count = 42;
        getDs().save(entity);

        FinalInConstructor loaded = getDs().find(FinalInConstructor.class).filter(eq("_id", entity.id)).first();
        assertEquals("final", loaded.name);
        assertEquals(42, loaded.count);
    }

    @Test
    public void prefersConstructorTakingFinals() {
        getMapper().map(ConstructorChoice.class);
        ConstructorChoice entity = new ConstructorChoice("final");
        entity.a = 1;
        entity.b = 2;
        getDs().save(entity);

        ConstructorChoice loaded = getDs().find(ConstructorChoice.class).filter(eq("_id", entity.id)).first();
        assertEquals("final", loaded.name);
        assertEquals(1, loaded.a);
        assertEquals(2, loaded.b);
    }

    private List<ConstraintViolation> check(Class<?> type, boolean denied) {
        Set<ConstraintViolation> violations = new HashSet<>();
        new FinalFieldsInConstructor(() -> denied)
                .check(getMapper(), new ReflectiveEntityModelBuilder(getMapper(), type).build(), violations);
        return List.copyOf(violations);
    }

    @Entity
    private static class FinalNotInConstructor {
        @Id
        ObjectId id;
        final String name;

        FinalNotInConstructor() {
            name = null;
        }
    }

    @Entity
    private static class FinalInConstructor {
        @Id
        ObjectId id;
        final String name;
        int count;

        FinalInConstructor(String name) {
            this.name = name;
        }
    }

    // the constructor taking the most properties leaves the final one out
    @Entity
    private static class ConstructorChoice {
        @Id
        ObjectId id;
        final String name;
        int a;
        int b;

        ConstructorChoice(int a, int b) {
            this.name = null;
            this.a = a;
            this.b = b;
        }

        ConstructorChoice(String name) {
            this.name = name;
        }
    }

    @Entity
    private static class LifecycleWithFinal {
        @Id
        ObjectId id;
        final String name;

        LifecycleWithFinal() {
            name = null;
        }

        LifecycleWithFinal(String name) {
            this.name = name;
        }

        @PrePersist
        void prePersist() {
        }
    }
}
