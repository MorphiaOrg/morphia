package com.google.code.morphia.issue485;

import org.junit.Test;

import com.google.code.morphia.TestBase;

public class AbstractVersionAnnotationTest extends TestBase {
	public AbstractVersionAnnotationTest() {
	}
	
	@Test
	public void abstractVersionTest() {
		// Abstract class mapping
		morphia.map(BaseFoo.class);
		
		// Non abstract class mapping
		morphia.map(Foo.class);
	}
}
