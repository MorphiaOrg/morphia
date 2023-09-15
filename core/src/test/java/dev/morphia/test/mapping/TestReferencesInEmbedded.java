package dev.morphia.test.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Reference;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.TestReferences.ReferencedEntity;
import dev.morphia.test.models.TestEntity;

import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;

@Test(groups = "references")
public class TestReferencesInEmbedded extends TestBase {
    public void testLazyReferencesInEmbedded() {
        final ContainerEntity container = new ContainerEntity();
        container.name = "lazy";
        getDs().save(container);
        final ReferencedEntity referencedEntity = new ReferencedEntity();
        getDs().save(referencedEntity);

        container.embed = new EmbedContainingReference();
        container.embed.lazyRef = referencedEntity;
        getDs().save(container);

        Assert.assertNotNull(getDs().find(ContainerEntity.class)
                .filter(eq("_id", container.getId()))
                .first());
    }

    @Entity
    private static class ContainerEntity extends TestEntity {
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
}
