package org.mongodb.morphia.issue488;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;

public class VersionedUpdateTest extends TestBase {

    private static class TestEntity {
        @Id
        private ObjectId id;
        @Version
        private Long version;
        private String name;
    }

    @Test
    public void versionedUpdate() {
        final TestEntity t = new TestEntity();
        t.name = "foo";

        this.getDs().save(t);

        final TestEntity t1 = getDs().get(TestEntity.class, t.id);
        t1.name = "bar";

        this.getDs().merge(t1);

        final TestEntity t2 = this.getDs().get(TestEntity.class, t.id);
        Assert.assertEquals(t1.name, t2.name);
    }
}
