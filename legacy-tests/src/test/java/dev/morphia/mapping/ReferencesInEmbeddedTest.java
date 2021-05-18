package dev.morphia.mapping;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Reference;
import dev.morphia.testmodel.TestEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static dev.morphia.query.experimental.filters.Filters.eq;


@Category(Reference.class)
public class ReferencesInEmbeddedTest extends TestBase {
    @Test
    public void testLazyReferencesInEmbedded() {
        final Container container = new Container();
        container.name = "lazy";
        getDs().save(container);
        final ReferencedEntity referencedEntity = new ReferencedEntity();
        getDs().save(referencedEntity);

        container.embed = new EmbedContainingReference();
        container.embed.lazyRef = referencedEntity;
        getDs().save(container);

        Assert.assertNotNull(getDs().find(Container.class)
                                    .filter(eq("_id", container.getId()))
                                    .first());
    }

    @Test
    public void testMapping() {
        getMapper().map(Container.class);
        getMapper().map(ReferencedEntity.class);
    }

    @Test
    public void testNonLazyReferencesInEmbedded() {
        final Container container = new Container();
        container.name = "nonLazy";
        getDs().save(container);
        final ReferencedEntity referencedEntity = new ReferencedEntity();
        getDs().save(referencedEntity);

        container.embed = new EmbedContainingReference();
        container.embed.ref = referencedEntity;
        getDs().save(container);

        Assert.assertNotNull(getDs().find(Container.class)
                                    .filter(eq("_id", container.getId()))
                                    .first());
    }

    @Entity
    private static class Container extends TestEntity {
        private String name;
        private EmbedContainingReference embed;
    }

    @Entity
    private static class EmbedContainingReference {
        private String name;
        @Reference
        private ReferencedEntity ref;

        @Reference(lazy = true)
        private ReferencedEntity lazyRef;
    }

    @Entity
    public static class ReferencedEntity extends TestEntity {
        private String foo;
    }
}
