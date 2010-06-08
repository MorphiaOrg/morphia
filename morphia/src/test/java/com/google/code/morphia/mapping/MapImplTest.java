/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class MapImplTest extends TestBase{
	

	@SuppressWarnings("unchecked")
	public static class E {
		@Id
		String id;

		@Embedded
		MyMap<String,String> mymap = new MyMap();
		
		
	}
	public static class MyMap<V,T> extends HashMap<String,String>{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
	}
	
	@Test
	public void testMapping() throws Exception {
		E e = new E();
		e.mymap.put("1", "a");
		e.mymap.put("2", "b");
		
		ds.save(e);
		
		e = ds.get(e);
		Assert.assertEquals("a", e.mymap.get("1"));
		Assert.assertEquals("b", e.mymap.get("2"));
	}
	
}
