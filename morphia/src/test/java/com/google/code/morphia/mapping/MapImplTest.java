/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.HashMap;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class MapImplTest extends TestBase{
	

	public static class E {
		@Id ObjectId id;

		@Embedded
		MyMap<String,String> mymap = new MyMap<String,String>();
	}
	public static class MyMap<V,T> extends HashMap<String,String>{
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
