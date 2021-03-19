package dev.morphia.test.mapping.codec.pojo;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.TestBase;

public class DiscriminantLifecycleTest extends TestBase {

    @Test
    public void testEntity() {
        ChildEntity childEntity = new ChildEntity();
        childEntity.value = "value";
        getDs().save(childEntity);
        ObjectId id = childEntity.id;
        BaseEntity saved = getDs().find(BaseEntity.class).filter(Filters.eq("_id", id)).first();
        Assert.assertTrue(saved instanceof ChildEntity);
        Assert.assertTrue(saved.audited);
    }

    @Entity(value = "entity")
    static class BaseEntity{
        @Id
        ObjectId id;
        @Transient
        boolean audited;

        @PostLoad
        void audit() {
            // audit entity
            audited = true;
        }
    }

    static class ChildEntity extends BaseEntity {
        String value;
    }
}