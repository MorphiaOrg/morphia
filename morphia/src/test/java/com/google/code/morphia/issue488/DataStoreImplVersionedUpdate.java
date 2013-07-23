package com.google.code.morphia.issue488;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Version;

public class DataStoreImplVersionedUpdate extends TestBase {

	private static class TestEntity {
		@Id
		ObjectId id;
		@Version
		Long version;
		String name;
	}

	@Test
	public void itShouldUseCorrectIdForVersionedUpdate() {
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
