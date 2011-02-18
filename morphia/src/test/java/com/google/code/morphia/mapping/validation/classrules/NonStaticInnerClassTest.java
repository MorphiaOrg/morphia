/**
 * 
 */
package com.google.code.morphia.mapping.validation.classrules;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class NonStaticInnerClassTest extends TestBase {
	
	static class Valid {
		@Id
		ObjectId id;
	}
	
	class InValid {
		@Id
		ObjectId id;
	}
	
	@Test
	public void testValidInnerClass() throws Exception {
		morphia.map(Valid.class);
	}
	
	@Test
	public void testInValidInnerClass() throws Exception {
		new AssertedFailure(ConstraintViolationException.class) {
			@Override
			protected void thisMustFail() throws Throwable {
				morphia.map(InValid.class);
			}
		};
	}
}
