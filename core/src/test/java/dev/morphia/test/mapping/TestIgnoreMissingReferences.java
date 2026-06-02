package dev.morphia.test.mapping;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

@Tag("references")
public class TestIgnoreMissingReferences extends TestBase {
    @Test
    public void testMissingReference() {
        final Container c = new Container();
        c.refs = new StringHolder[] { new StringHolder(), new StringHolder() };
        getDs().save(c);
        getDs().save(c.refs[0]);

        Container reloadedContainer = getDs().find(Container.class).iterator()
                .tryNext();
        Assertions.assertNotNull(reloadedContainer);
        Assertions.assertNotNull(reloadedContainer.refs);
        Assertions.assertEquals(1, reloadedContainer.refs.length);

        reloadedContainer = getDs().find(Container.class)
                .filter(eq("_id", c.id))
                .first();
        Assertions.assertNotNull(reloadedContainer);
        Assertions.assertNotNull(reloadedContainer.refs);
        Assertions.assertEquals(1, reloadedContainer.refs.length);

        final List<Container> cs = getDs().find(Container.class).iterator().toList();
        Assertions.assertNotNull(cs);
        Assertions.assertEquals(1, cs.size());

    }

    @Entity(discriminator = "missingReferences")
    private static class Container {
        @Id
        private ObjectId id;
        @Reference(ignoreMissing = true)
        private StringHolder[] refs;
    }

    @Entity
    private static class StringHolder {
        @Id
        private final ObjectId id = new ObjectId();
    }
}
