/**
 * 
 */
package com.google.code.morphia.ext.guice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Transient;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * @author us@thomas-daily.de
 */
public class GuiceTest extends TestBase {
	private Injector i;
	
	@Override
	public void setUp() {
		
		super.setUp();
		this.i = Guice.createInjector(new AbstractModule() {
			
			@Override
			protected void configure() {
				bind(Foo.class).toInstance(new Bar());
			}
		});
		new GuiceExtension(this.morphia, this.i);
		this.morphia.map(E1.class);
		this.morphia.map(E2.class);
	}
	
	@Test
	public void testE1() throws Exception {
		
		final E1 initialEntity = this.i.getInstance(E1.class);
		final Key<E1> k = this.ds.save(initialEntity);
		final E1 loadedEntity = this.ds.getByKey(E1.class, k);
		
		Assert.assertNotNull(loadedEntity);
		Assert.assertNotNull(loadedEntity.foo);
		Assert.assertEquals(Bar.class, loadedEntity.foo.getClass());
		Assert.assertEquals(ArrayList.class, loadedEntity.l.getClass());
		
	}
	
	@Test
	public void testE2() throws Exception {
		
		final E2 initialEntity = new E2();
		final Key<E2> k = this.ds.save(initialEntity);
		final E2 loadedEntity = this.ds.getByKey(E2.class, k);
		
		Assert.assertNotNull(loadedEntity);
		
	}
	
	@Test
	public void testE3() throws Exception {
		
		final E3 initialEntity = this.i.getInstance(E3.class);
		final Key<E3> k = this.ds.save(initialEntity);
		final E3 loadedEntity = this.ds.getByKey(E3.class, k);
		
		Assert.assertNotNull(loadedEntity);
		Assert.assertEquals(MyList.class, loadedEntity.l.getClass());
		Assert.assertNotNull(loadedEntity.l.foo);
		
	}
	
	@Test
	public void testE4() throws Exception {
		
		final E4 initialEntity = this.i.getInstance(E4.class);
		final Key<E4> k = this.ds.save(initialEntity);
		final E4 loadedEntity = this.ds.getByKey(E4.class, k);
		
		Assert.assertNotNull(loadedEntity);
		Assert.assertNotNull(loadedEntity.foo);
		
	}
	
	public interface Foo {
		int sum(int a, int b);
	}
	
	static class Bar implements Foo {
		public int sum(final int a, final int b) {
			return a + b;
		}
	}
	
	@Entity
	public static class E1 {
		@Id
		ObjectId id;
		
		@Transient
		Foo foo;
		
		List<Integer> l = Arrays.asList(1, 3, 4);
		
		@Inject
		E1(final Foo f) {
			this.foo = f;
		}
		
		String s = "";
	}
	
	@Entity
	public static class E2 {
		@Id
		ObjectId id;
		
		String s = "";
	}
	
	@Entity
	public static class E3 {
		@Id
		ObjectId id;
		
		MyList<Integer> l = new MyList<Integer>(new Bar());
		
		String s = "";
		
		/**
		 * 
		 */
		public E3() {
			this.l.add(1);
			this.l.add(2);
		}
	}
	
	@Entity
	public static class E4 {
		@Id
		ObjectId id;
		
		String s = "";
		
		@Inject
		@Transient
		Foo foo;
	}
	
	static class MyList<E> extends ArrayList<E> {
		Foo foo;
		
		@Inject
		public MyList(final Foo foo) {
			this.foo = foo;
		}
	}
	
}
