package com.google.code.morphia;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.Reference;

/**
 * Tests mapper functions; this is tied to some of the internals. 
 * @author scotthernandez
 *
 */
public class TestMapper extends TestBase {
	public static class A {
		static int loadCount = 0;
		@Id String id;
		
		String getId() {
			return id;
		}
		
		@PostLoad
		protected void postConstruct() {
			if (loadCount > 1) 
				throw new RuntimeException();
			
			loadCount++;
		}
	}
	
	@Entity("holders")
	public static class HoldsMultipleA{
		@Id String id;
		@Reference A a1;
		@Reference A a2;
	}

	@Entity("holders")
	public static class HoldsMultipleALazily{
		@Id String id;
		@Reference(lazy=true) A a1;
		@Reference A a2;
		@Reference(lazy=true) A a3;
	}
	
    @Test
    public void SingleLookup() throws Exception {
    	A a = new A();
    	HoldsMultipleA holder = new HoldsMultipleA();
    	holder.a1 = a;
    	holder.a2 = a;
    	ds.save(a, holder);
    	holder = ds.get(HoldsMultipleA.class, holder.id);
    	Assert.assertEquals(1, A.loadCount);
    	Assert.assertTrue(holder.a1 == holder.a2);
    }

    @Test
    public void SingleProxy() throws Exception {
    	A.loadCount=0;
    	A a = new A();
    	HoldsMultipleALazily holder = new HoldsMultipleALazily();
    	holder.a1 = a;
    	holder.a2 = a;
    	holder.a3 = a;
    	ds.save(a, holder);
    	holder = ds.get(HoldsMultipleALazily.class, holder.id);
    	Assert.assertEquals(1, A.loadCount);
    	Assert.assertFalse(holder.a1 == holder.a2);
    	Assert.assertTrue(holder.a1 == holder.a3);
//    	A.loadCount=0;
//    	Assert.assertEquals(holder.a1.getId(), holder.a2.getId());
    	
    }
}
