package com.google.code.morphia.logging.slf4j;


import org.junit.Before;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.MongoClient;

public abstract class TestBase {

	protected MongoClient mongo;
	protected DB db;
	protected Datastore ds;
	protected Morphia morphia;

	@Before
	public void setUp() {
		try {
			this.mongo = new MongoClient();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		this.mongo.dropDatabase("morphia_test");
		morphia = new Morphia();
		this.db = this.mongo.getDB("morphia_test");
		this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
	}
}
