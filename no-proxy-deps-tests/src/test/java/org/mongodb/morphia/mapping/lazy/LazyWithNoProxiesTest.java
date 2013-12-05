package org.mongodb.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.ArrayList;
import java.util.List;


public class LazyWithNoProxiesTest extends ProxyTestBase {
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
        
        getDs().save(otherEntity1, entity);

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
}
