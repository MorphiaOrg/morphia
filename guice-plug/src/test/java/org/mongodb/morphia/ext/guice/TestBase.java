package org.mongodb.morphia.ext.guice;

import org.junit.After;
import org.junit.Before;

import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;


public abstract class TestBase {
	protected Mongo mongo;
	protected DB db;
	protected Datastore ds;
	protected AdvancedDatastore ads;
	protected Morphia morphia = new Morphia();

	protected TestBase() {
		try {
			this.mongo = new MongoClient();
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
