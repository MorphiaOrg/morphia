package org.mongodb.morphia.logging.slf4j;


import org.junit.Before;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;


public abstract class TestBase {

	protected Mongo mongo;
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
