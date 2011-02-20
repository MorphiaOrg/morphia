/**
 * 
 */
package com.google.code.morphia.ext.guice;

import org.junit.After;
import org.junit.Before;

import com.google.code.morphia.AdvancedDatastore;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.MappedClass;
import com.mongodb.DB;
import com.mongodb.Mongo;

public abstract class TestBase {
	protected Mongo mongo;
	protected DB db;
	protected Datastore ds;
	protected AdvancedDatastore ads;
	protected Morphia morphia = new Morphia();
	
	protected TestBase() {
		try {
			this.mongo = new Mongo();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Before
	public void setUp() {
		this.db = this.mongo.getDB("morphia_test");
		this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
		this.ads = (AdvancedDatastore) this.ds;
	}
	
	protected void dropDB() {
		// this.mongo.dropDatabase("morphia_test");
		for (final MappedClass mc : this.morphia.getMapper().getMappedClasses()) {
			// if( mc.getEntityAnnotation() != null )
			this.db.getCollection(mc.getCollectionName()).drop();
		}
		
	}
	
	@After
	public void tearDown() {
		dropDB();
	}
}
