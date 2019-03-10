package dev.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.testutil.TestEntity;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;


public class LazyWithNoProxiesTest extends ProxyTestBase {
    @Test
    public void testLoadingOfRefInField() throws Exception {
        getMorphia().map(EmbedWithRef.class);
        getMorphia().map(OtherEntity.class);

        EmbedWithRef entity = new EmbedWithRef();
        OtherEntity otherEntity1 = new OtherEntity(SomeEnum.B);
        int count = 10;
        for (int x = 0; x < count; x++) {
            OtherEntity item = new OtherEntity(SomeEnum.A);
            getDs().save(item);
            entity.list.add(item);
        }
        entity.otherEntity = otherEntity1;

        getDs().save(asList(otherEntity1, entity));

        OtherEntity loadedOther = getDs().get(otherEntity1);
        EmbedWithRef loadedEntity = getDs().get(entity);
        Assert.assertNotNull(loadedOther);
        Assert.assertNotNull(loadedEntity);
        assertNotProxy(loadedEntity.otherEntity);
        Assert.assertEquals(count, loadedEntity.list.size());
        for (OtherEntity item : loadedEntity.list) {
            assertNotProxy(item);
        }
    }

    public enum SomeEnum {
        B,
        A
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

    public static class EmbedWithRef extends TestEntity {

        @Reference(lazy = true)
        private OtherEntity otherEntity;

        @Reference(lazy = true)
        private List<OtherEntity> list = new ArrayList<OtherEntity>();
    }
}
