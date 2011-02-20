/**
 * 
 */
package com.google.code.morphia.query;

import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.testutil.AssertedFailure;


public class TestMandatoryId extends TestBase {
	
	@Entity
	public static class E {
		// not id here
		String foo = "bar";
	}
	
	@Test
	public final void testMissingId() {
		new AssertedFailure() {
			
			@Override
			protected void thisMustFail() throws Throwable {
				morphia.map(E.class);
			}
		};
	}
	
	@Test
	public final void testMissingIdNoImplicitMapCall() {
		Key<E> save = ds.save(new E());
		E byKey = ds.getByKey(E.class, save);
	}

}
