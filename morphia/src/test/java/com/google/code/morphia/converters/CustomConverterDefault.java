/**
 * 
 */
package com.google.code.morphia.converters;

import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.testutil.TestEntity;

/**
 * @author Uwe Schaefer
 * 
 */
public class CustomConverterDefault extends TestBase {
	
	public static class E extends TestEntity {
		private static final long serialVersionUID = 1L;
		
		// FIXME issue 100 :
		// http://code.google.com/p/morphia/issues/detail?id=100
		// check default inspection: if not declared as property,
		// morphia fails due to defaulting to embedded and expecting a non-arg
		// constructor.
		//
		// @Property
		Foo foo = new Foo("test");
		
	}
	
	// unknown type to convert
	public static class Foo {
		private String string;
		
		public Foo(String string) {
			this.string = string;
		}
		
		@Override
		public String toString() {
			return string;
		}
	}
	
	public static class FooConverter extends TypeConverter {
		
		public boolean done;
		
		public FooConverter() {
			super(Foo.class);
		}
		
		@Override
		public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo)
				throws MappingException {
			return new Foo((String) fromDBObject);
		}
		
		@Override
		public Object encode(Object value, MappedField optionalExtraInfo) {
			done = true;
			return value.toString();
		}
		
		public boolean didConversion() {
			return done;
		}
	}
	
	@Test
	@Ignore
	public void testConversion() throws Exception {
		FooConverter fc = new FooConverter();
		morphia.getMapper().getConverters().addConverter(fc);
		E e = new E();
		ds.save(e);
		
		junit.framework.Assert.assertTrue(fc.didConversion());
	}
	
}
