package org.mongodb.morphia.mapping.lazy;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.testutil.TestEntity;
import org.junit.Assert;


/**
 * @author josephpachod
 */
public class LazyInEmbeddedTest extends TestBase {
  public enum SomeEnum {
    B,
    A
  }

  @Entity
  public static class ContainerWithRefInField extends TestEntity {
    @Embedded
    private EmbedWithRef embedWithRef;
  }

  @Entity
  public static class ContainerWithRefList extends TestEntity {
    @Embedded
    private final List<EmbedWithRef> embedWithRef = new ArrayList<EmbedWithRef>();
  }

  @Entity
  public static class OtherEntity extends TestEntity {
    @Property(value = "some")
    private SomeEnum someEnum;

    protected OtherEntity() {
    }

    public OtherEntity(final SomeEnum someEnum) {
      this.someEnum = someEnum;

    }
  }

  @Entity
  public static class OtherEntityChild extends OtherEntity {
    public OtherEntityChild() {
      super(SomeEnum.A);
    }

    @Property
    private String name;
  }

  public static class EmbedWithRef implements Serializable {

    @Reference(lazy = true)
    private OtherEntity otherEntity;
  }

  @Test
  public void testLoadingOfRefInField() throws Exception {
    // TODO us: exclusion does not work properly with maven + junit4
    if (!LazyFeatureDependencies.testDependencyFullFilled()) {
      return;
    }

    morphia.map(ContainerWithRefInField.class);
    morphia.map(OtherEntity.class);

    OtherEntity otherEntity = new OtherEntity();
    ContainerWithRefInField containerWithRefInField = new ContainerWithRefInField();

    ds.save(otherEntity, containerWithRefInField);

    otherEntity = ds.get(otherEntity);
    containerWithRefInField = ds.get(containerWithRefInField);
    Assert.assertNotNull(otherEntity);
    Assert.assertNotNull(containerWithRefInField);

    final EmbedWithRef embedWithRef = new EmbedWithRef();
    embedWithRef.otherEntity = otherEntity;
    containerWithRefInField.embedWithRef = embedWithRef;

    ds.save(containerWithRefInField);

    containerWithRefInField = ds.get(containerWithRefInField);
    Assert.assertNotNull(containerWithRefInField);

  }

  @Test
  public void testLoadingOfRefThroughInheritanceInField() throws Exception {
    // TODO us: exclusion does not work properly with maven + junit4
    if (!LazyFeatureDependencies.testDependencyFullFilled()) {
      return;
    }

    morphia.map(ContainerWithRefInField.class);
    morphia.map(OtherEntityChild.class);

    OtherEntityChild otherEntity = new OtherEntityChild();
    ContainerWithRefInField containerWithRefInField = new ContainerWithRefInField();

    ds.save(otherEntity, containerWithRefInField);

    otherEntity = ds.get(otherEntity);
    final ContainerWithRefInField reload = ds.get(containerWithRefInField);
    Assert.assertNotNull(otherEntity);
    Assert.assertNotNull(reload);

    final EmbedWithRef embedWithRef = new EmbedWithRef();
    embedWithRef.otherEntity = otherEntity;
    reload.embedWithRef = embedWithRef;

    ds.save(reload);
    ds.get(reload);
    containerWithRefInField = ds.get(containerWithRefInField);
    Assert.assertNotNull(containerWithRefInField);

  }

  @Test
  public void testLoadingOfRefInList() throws Exception {
    // TODO us: exclusion does not work properly with maven + junit4
    if (!LazyFeatureDependencies.testDependencyFullFilled()) {
      return;
    }

    morphia.map(ContainerWithRefList.class);
    morphia.map(OtherEntity.class);

    OtherEntity otherEntity = new OtherEntity();
    ContainerWithRefList containerWithRefInList = new ContainerWithRefList();

    ds.save(otherEntity, containerWithRefInList);

    otherEntity = ds.get(otherEntity);
    containerWithRefInList = ds.get(containerWithRefInList);
    Assert.assertNotNull(otherEntity);
    Assert.assertNotNull(containerWithRefInList);

    final EmbedWithRef embedWithRef = new EmbedWithRef();
    embedWithRef.otherEntity = otherEntity;
    containerWithRefInList.embedWithRef.add(embedWithRef);

    ds.save(otherEntity, containerWithRefInList);

    containerWithRefInList = ds.get(containerWithRefInList);
    Assert.assertNotNull(containerWithRefInList);

    final Query<ContainerWithRefList> createQuery = ds.createQuery(ContainerWithRefList.class);
    containerWithRefInList = createQuery.get();
    Assert.assertNotNull(containerWithRefInList);

  }

  @Test
  public void testLoadingOfRefThroughInheritanceInList() throws Exception {
    // TODO us: exclusion does not work properly with maven + junit4
    if (!LazyFeatureDependencies.testDependencyFullFilled()) {
      return;
    }

    morphia.map(ContainerWithRefList.class);
    morphia.map(OtherEntityChild.class);

    OtherEntityChild otherEntity = new OtherEntityChild();
    ContainerWithRefList containerWithRefInList = new ContainerWithRefList();

    ds.save(otherEntity, containerWithRefInList);

    otherEntity = ds.get(otherEntity);
    final ContainerWithRefList reload = ds.get(containerWithRefInList);
    Assert.assertNotNull(otherEntity);
    Assert.assertNotNull(reload);

    final EmbedWithRef embedWithRef = new EmbedWithRef();
    embedWithRef.otherEntity = otherEntity;
    reload.embedWithRef.add(embedWithRef);

    ds.save(otherEntity, reload);

    ds.get(reload);

    containerWithRefInList = ds.get(reload);
    Assert.assertNotNull(containerWithRefInList);
    final Query<ContainerWithRefList> createQuery = ds.createQuery(ContainerWithRefList.class);
    containerWithRefInList = createQuery.get();
    Assert.assertNotNull(containerWithRefInList);

  }
}
