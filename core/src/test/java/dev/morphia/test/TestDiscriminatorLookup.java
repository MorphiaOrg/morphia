package dev.morphia.test;

import dev.morphia.annotations.Entity;
import dev.morphia.test.models.TestEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDiscriminatorLookup extends TestBase {

    @Test
    public void testLookup() {
        withConfig(buildConfig(SomeEntity.class), () -> {
            final SomeEntity entity = new SomeEntity();
            entity.setShape(new Shape.Square());

            getDs().save(entity);
        });

        final SomeEntity entity = getDs().find(SomeEntity.class).first();
        Assertions.assertNotNull(entity);
        Assertions.assertTrue(Shape.Square.class.isInstance(entity.getShape()));
    }

    @Entity
    public static class SomeEntity extends TestEntity {
        private Shape shape;

        public Shape getShape() {
            return shape;
        }

        public void setShape(Shape shape) {
            this.shape = shape;
        }
    }

    @Entity
    public static abstract class Shape {
        public static class Square extends Shape {
            public double side;
        }
    }
}
