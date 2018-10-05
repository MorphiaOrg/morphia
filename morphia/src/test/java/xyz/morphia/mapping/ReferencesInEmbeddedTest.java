package xyz.morphia.mapping;


import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Reference;
import xyz.morphia.testutil.TestEntity;


/**
 * @author josephpachod
 */
public class ReferencesInEmbeddedTest extends TestBase {
    @Test
    public void testLazyReferencesInEmbedded() throws Exception {
        final Container container = new Container();
        container.name = "lazy";
        getDs().save(container);
        final ReferencedEntity referencedEntity = new ReferencedEntity();
        getDs().save(referencedEntity);

        container.embed = new EmbedContainingReference();
        container.embed.lazyRef = referencedEntity;
        getDs().save(container);

        final Container reloadedContainer = getDs().get(container);
        Assert.assertNotNull(reloadedContainer);
    }

    @Test
    public void testMapping() throws Exception {
        getMorphia().map(Container.class);
        getMorphia().map(ReferencedEntity.class);
    }

    @Test
    public void testNonLazyReferencesInEmbedded() throws Exception {
        final Container container = new Container();
        container.name = "nonLazy";
        getDs().save(container);
        final ReferencedEntity referencedEntity = new ReferencedEntity();
        getDs().save(referencedEntity);

        container.embed = new EmbedContainingReference();
        container.embed.ref = referencedEntity;
        getDs().save(container);

        final Container reloadedContainer = getDs().get(container);
        Assert.assertNotNull(reloadedContainer);
    }

    @Entity
    private static class Container extends TestEntity {
        private String name;
        @Embedded
        private EmbedContainingReference embed;
    }

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
