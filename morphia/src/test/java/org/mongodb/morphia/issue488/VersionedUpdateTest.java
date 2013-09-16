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
		ObjectId id;
		@Version
		Long version;
		String name;
	}

	@Test
	public void versionedUpdate() {
		TestEntity t = new TestEntity();
		t.name = "foo";

		this.ds.save(t);

		TestEntity t1 = this.ds.get(TestEntity.class, t.id);
		t1.name = "bar";

		this.ds.merge(t1);

		TestEntity t2 = this.ds.get(TestEntity.class, t.id);
		Assert.assertEquals(t1.name, t2.name);
	}
}
