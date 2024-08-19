package dev.morphia.test;

import dev.morphia.annotations.Entity;
import dev.morphia.test.models.Shape;
import dev.morphia.test.models.Square;
import dev.morphia.test.models.TestEntity;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestDiscriminatorLookup extends TestBase {

    @Test
    public void testLookup() {
        withConfig(buildConfig(SomeEntity.class), () -> {
            final SomeEntity entity = new SomeEntity();
            entity.setShape(new Square());

            getDs().save(entity);
        });

        final SomeEntity entity = getDs().find(SomeEntity.class).first();
        assertNotNull(entity);
        assertTrue(Square.class.isInstance(entity.getShape()));
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
}
