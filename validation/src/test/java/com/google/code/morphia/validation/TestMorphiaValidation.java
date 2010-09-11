/**
 * 
 */
package com.google.code.morphia.validation;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.Email;
import org.junit.Test;

import com.google.code.morphia.annotations.Id;

/**
 * @author doc
 */
public class TestMorphiaValidation extends TestBase {
	
	public static class E {
		@Id
		ObjectId id;
		@Email
		String email;
	}
	
	/**
	 * Test method for
	 * {@link com.google.code.morphia.validation.MorphiaValidation#prePersist(java.lang.Object, com.mongodb.DBObject, com.google.code.morphia.mapping.Mapper)}
	 * .
	 */
	@Test
	public final void testPrePersist() {
		final E e = new E();
		e.email = "not an email";
		
		new MorphiaValidation().applyTo(this.morphia);
		
		new AssertedFailure() {
			
			@Override
			protected void thisMustFail() throws Throwable {
				TestMorphiaValidation.this.ds.save(e);
			}
		};
		
		e.email = "foo@bar.com";
		this.ds.save(e);
		
	}
	
}
