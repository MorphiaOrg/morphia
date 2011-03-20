/**
 * 
 */
package com.google.code.morphia.ext.entityscanner;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.code.morphia.Morphia;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * @author us@thomas-daily.de
 * 
 */
public class EntityScannerTest extends TestCase {
	@Test
	public void testScanning() throws Exception {
		final Morphia m = new Morphia();
		assertFalse(m.isMapped(E.class));
		new EntityScanner(m, Predicates.equalTo(E.class.getName() + ".class"));
		assertTrue(m.isMapped(E.class));
		assertFalse(m.isMapped(F.class));
		new EntityScanner(m, new Predicate<String>() {
			
			public boolean apply(final String input) {
				return input.startsWith(EntityScannerTest.class.getPackage().getName());
			}
		});
		assertTrue(m.isMapped(F.class));
		
	}
	
}
