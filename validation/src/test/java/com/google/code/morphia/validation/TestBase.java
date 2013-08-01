package com.google.code.morphia.validation;

import org.junit.After;
import org.junit.Before;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.MongoClient;

public abstract class TestBase {
	protected MongoClient mongo;
	protected DB db;
	protected Datastore ds;
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
		this.mongo.dropDatabase("morphia_test");
		this.db = this.mongo.getDB("morphia_test");
		this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
	}

	@After
	public void tearDown() {
		// new ScopedFirstLevelCacheProvider().release();
	}
}
