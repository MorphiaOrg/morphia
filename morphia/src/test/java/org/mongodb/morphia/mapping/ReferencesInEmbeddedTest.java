package org.mongodb.morphia.mapping;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;
import org.junit.Assert;


/**
 * @author josephpachod
 */
public class ReferencesInEmbeddedTest extends TestBase {
  @Entity
  private static class Container extends TestEntity {
    private static final long serialVersionUID = 1L;
    String name;
    @Embedded
    private EmbedContainingReference embed;
  }

  private static class EmbedContainingReference {
    String name;
    @Reference
    protected ReferencedEntity ref;

    @Reference(lazy = true)
    protected ReferencedEntity lazyRef;
  }

  @Entity
  public static class ReferencedEntity extends TestEntity {
    private static final long serialVersionUID = 1L;
    String foo;
  }

  @Test
  public void testMapping() throws Exception {
    morphia.map(Container.class);
    morphia.map(ReferencedEntity.class);
  }

  @Test
  public void testNonLazyReferencesInEmbedded() throws Exception {
    final Container container = new Container();
    container.name = "nonLazy";
    ds.save(container);
    final ReferencedEntity referencedEntity = new ReferencedEntity();
    ds.save(referencedEntity);

    container.embed = new EmbedContainingReference();
    container.embed.ref = referencedEntity;
    ds.save(container);

    final Container reloadedContainer = ds.get(container);
    Assert.assertNotNull(reloadedContainer);
  }

  @Test
  public void testLazyReferencesInEmbedded() throws Exception {
    final Container container = new Container();
    container.name = "lazy";
    ds.save(container);
    final ReferencedEntity referencedEntity = new ReferencedEntity();
    ds.save(referencedEntity);

    container.embed = new EmbedContainingReference();
    container.embed.lazyRef = referencedEntity;
    ds.save(container);

    final Container reloadedContainer = ds.get(container);
    Assert.assertNotNull(reloadedContainer);
  }
}
