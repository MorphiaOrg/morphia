package dev.morphia.mapping.lazy;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.testmodel.TestEntity;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static dev.morphia.internal.MorphiaInternals.proxyClassesPresent;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.util.Arrays.asList;


@Category(Reference.class)
public class LazyInEmbeddedTest extends TestBase {
    @Test
    public void testLoadingOfRefInField() {
        Assume.assumeTrue(proxyClassesPresent());

        getMapper().map(ContainerWithRefInField.class);
        getMapper().map(OtherEntity.class);

        OtherEntity otherEntity = new OtherEntity();
        ContainerWithRefInField containerWithRefInField = new ContainerWithRefInField();

        getDs().save(asList(otherEntity, containerWithRefInField));

        otherEntity = getDs().find(OtherEntity.class)
                             .filter(eq("_id", otherEntity.getId()))
                             .first();
        containerWithRefInField = getDs().find(ContainerWithRefInField.class)
                                         .filter(eq("_id", containerWithRefInField.getId()))
                                         .first();
        Assert.assertNotNull(otherEntity);
        Assert.assertNotNull(containerWithRefInField);

        final EmbedWithRef embedWithRef = new EmbedWithRef();
        embedWithRef.otherEntity = otherEntity;
        containerWithRefInField.embedWithRef = embedWithRef;

        getDs().save(containerWithRefInField);

        Assert.assertNotNull(getDs().find(ContainerWithRefInField.class)
                                    .filter(eq("_id", containerWithRefInField.getId()))
                                    .first());

    }

    @Test
    public void testLoadingOfRefInList() {
        Assume.assumeTrue(proxyClassesPresent());

        getMapper().map(ContainerWithRefList.class);
        getMapper().map(OtherEntity.class);

        OtherEntity otherEntity = new OtherEntity();
        ContainerWithRefList containerWithRefInList = new ContainerWithRefList();

        getDs().save(asList(otherEntity, containerWithRefInList));

        otherEntity = getDs().find(OtherEntity.class)
                             .filter(eq("_id", otherEntity.getId()))
                             .first();
        containerWithRefInList = getDs().find(ContainerWithRefList.class)
                                        .filter(eq("_id", containerWithRefInList.getId()))
                                        .first();
        Assert.assertNotNull(otherEntity);
        Assert.assertNotNull(containerWithRefInList);

        final EmbedWithRef embedWithRef = new EmbedWithRef();
        embedWithRef.otherEntity = otherEntity;
        containerWithRefInList.embedWithRef.add(embedWithRef);

        getDs().save(asList(otherEntity, containerWithRefInList));

        containerWithRefInList = getDs().find(ContainerWithRefList.class)
                                        .filter(eq("_id", containerWithRefInList.getId()))
                                        .first();
        Assert.assertNotNull(containerWithRefInList);

        final Query<ContainerWithRefList> createQuery = getDs().find(ContainerWithRefList.class);
        containerWithRefInList = createQuery.iterator(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(containerWithRefInList);

    }

    @Test
    public void testLoadingOfRefThroughInheritanceInField() {
        Assume.assumeTrue(proxyClassesPresent());

        getMapper().map(ContainerWithRefInField.class);
        getMapper().map(OtherEntityChild.class);

        OtherEntityChild otherEntity = new OtherEntityChild();
        ContainerWithRefInField containerWithRefInField = new ContainerWithRefInField();

        getDs().save(asList(otherEntity, containerWithRefInField));

        otherEntity = getDs().find(OtherEntityChild.class)
                             .filter(eq("_id", otherEntity.getId()))
                             .first();
        ContainerWithRefInField reload = getDs().find(ContainerWithRefInField.class)
                                                .filter(eq("_id", containerWithRefInField.getId()))
                                                .first();
        Assert.assertNotNull(otherEntity);
        Assert.assertNotNull(reload);

        final EmbedWithRef embedWithRef = new EmbedWithRef();
        embedWithRef.otherEntity = otherEntity;
        reload.embedWithRef = embedWithRef;

        getDs().save(reload);
        getDs().find(ContainerWithRefInField.class)
               .filter(eq("_id", reload.getId()))
               .first();
        containerWithRefInField = getDs().find(ContainerWithRefInField.class)
                                         .filter(eq("_id", containerWithRefInField.getId()))
                                         .first();
        Assert.assertNotNull(containerWithRefInField);

    }

    @Test
    public void testLoadingOfRefThroughInheritanceInList() {
        Assume.assumeTrue(proxyClassesPresent());

        getMapper().map(ContainerWithRefList.class);
        getMapper().map(OtherEntityChild.class);

        OtherEntityChild otherEntity = new OtherEntityChild();
        ContainerWithRefList containerWithRefInList = new ContainerWithRefList();

        getDs().save(asList(otherEntity, containerWithRefInList));

        otherEntity = getDs().find(OtherEntityChild.class)
                             .filter(eq("_id", otherEntity.getId()))
                             .first();
        final ContainerWithRefList reload = getDs().find(ContainerWithRefList.class)
                                                   .filter(eq("_id", containerWithRefInList.getId()))
                                                   .first();
        Assert.assertNotNull(otherEntity);
        Assert.assertNotNull(reload);

        final EmbedWithRef embedWithRef = new EmbedWithRef();
        embedWithRef.otherEntity = otherEntity;
        reload.embedWithRef.add(embedWithRef);

        getDs().save(asList(otherEntity, reload));

        containerWithRefInList = getDs().find(ContainerWithRefList.class)
                                        .filter(eq("_id", reload.getId()))
                                        .first();
        Assert.assertNotNull(containerWithRefInList);
        final Query<ContainerWithRefList> createQuery = getDs().find(ContainerWithRefList.class);
        containerWithRefInList = createQuery.iterator(new FindOptions().limit(1)).tryNext();
        Assert.assertNotNull(containerWithRefInList);

    }

    public enum SomeEnum {
        B,
        A
    }

    @Entity
    public static class ContainerWithRefInField extends TestEntity {
        private EmbedWithRef embedWithRef;
    }

    @Entity
    public static class ContainerWithRefList extends TestEntity {
        private final List<EmbedWithRef> embedWithRef = new ArrayList<>();
    }

    @Entity("other")
    public static class OtherEntity extends TestEntity {
        @Property(value = "some")
        private SomeEnum someEnum;

        protected OtherEntity() {
        }

        public OtherEntity(SomeEnum someEnum) {
            this.someEnum = someEnum;

        }
    }

    public static class OtherEntityChild extends OtherEntity {
        @Property
        private String name;

        public OtherEntityChild() {
            super(SomeEnum.A);
        }
    }

    @Entity
    public static class EmbedWithRef implements Serializable {

        @Reference(lazy = true)
        private OtherEntity otherEntity;
    }
}
