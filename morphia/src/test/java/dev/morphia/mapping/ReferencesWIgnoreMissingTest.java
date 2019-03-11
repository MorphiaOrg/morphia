package dev.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.query.FindOptions;

import java.util.List;


/**
 * @author scotthernandez
 */
public class ReferencesWIgnoreMissingTest extends TestBase {
    @Test
    public void testMissingReference() {
        final Container c = new Container();
        c.refs = new StringHolder[]{new StringHolder(), new StringHolder()};
        getDs().save(c);
        getDs().save(c.refs[0]);

        Container reloadedContainer = getDs().find(Container.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(reloadedContainer);
        Assert.assertNotNull(reloadedContainer.refs);
        Assert.assertEquals(1, reloadedContainer.refs.length);

        reloadedContainer = getDs().get(c);
        Assert.assertNotNull(reloadedContainer);
        Assert.assertNotNull(reloadedContainer.refs);
        Assert.assertEquals(1, reloadedContainer.refs.length);

        final List<Container> cs = toList(getDs().find(Container.class).find());
        Assert.assertNotNull(cs);
        Assert.assertEquals(1, cs.size());

    }

    @Entity
    static class Container {
        @Id
        private ObjectId id;
        @Reference(ignoreMissing = true)
        private StringHolder[] refs;
    }

    @Entity
    static class StringHolder {
        @Id
        private ObjectId id = new ObjectId();
    }
}
