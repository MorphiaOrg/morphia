package dev.morphia.test.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Test(groups = "references")
public class TestIgnoreMissingReferences extends TestBase {
    public void testMissingReference() {
        final Container c = new Container();
        c.refs = new StringHolder[]{new StringHolder(), new StringHolder()};
        getDs().save(c);
        getDs().save(c.refs[0]);

        Container reloadedContainer = getDs().find(Container.class).iterator(new FindOptions().limit(1))
                                             .tryNext();
        assertNotNull(reloadedContainer);
        assertNotNull(reloadedContainer.refs);
        assertEquals(reloadedContainer.refs.length, 1);

        reloadedContainer = getDs().find(Container.class)
                                   .filter(eq("_id", c.id))
                                   .first();
        assertNotNull(reloadedContainer);
        assertNotNull(reloadedContainer.refs);
        assertEquals(reloadedContainer.refs.length, 1);

        final List<Container> cs = getDs().find(Container.class).iterator().toList();
        assertNotNull(cs);
        assertEquals(cs.size(), 1);

    }

    @Entity
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
