package com.google.code.morphia.query;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.TestMapping.BaseEntity;
import com.google.code.morphia.annotations.Entity;

public class TestStartsWithIgnoreCase extends TestBase {
	@Entity
	static class E extends BaseEntity {
		final String name;
		
		public E(String name) {
			this.name = name;
		}
		
		protected E() {
			name = null;
		}
	}
	
	@Test
	public void testNullAcceptance() throws Exception {
		
		ds.save(new E("A"), new E("a"), new E("Ab"), new E("ab"), new E("c"));
		
		Assert.assertEquals(2, ds.createQuery(E.class).field("name").startsWith("a").countAll());
		Assert.assertEquals(4, ds.createQuery(E.class).field("name").startsWithIgnoreCase("a").countAll());
		Assert.assertEquals(4, ds.createQuery(E.class).field("name").startsWithIgnoreCase("A").countAll());
		
	}
}
