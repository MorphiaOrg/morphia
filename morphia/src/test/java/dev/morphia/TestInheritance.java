package dev.morphia;

import org.bson.types.ObjectId;
import org.junit.Test;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestInheritance extends TestBase {
    @Test
    public void testSavingAndLoadingAClassWithDeepInheritance() {
        // given
        final Child jimmy = new Child();
        jimmy.setName("jimmy");
        getDs().save(jimmy);

        // when
        final Child loaded = getDs().get(Child.class, jimmy.getId());

        // then
        assertNotNull(loaded);
        assertEquals(jimmy.getName(), loaded.getName());
    }

    @Entity
    public static class Child extends Father {
    }

    @Entity
    public static class Father extends GrandFather {
    }

    @Entity
    public static class GrandFather {
        @Id
        private ObjectId id;
        private String name;

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
