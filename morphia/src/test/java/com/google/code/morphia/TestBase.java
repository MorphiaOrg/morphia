package com.google.code.morphia;


import org.junit.After;
import org.junit.Before;
import com.google.code.morphia.mapping.MappedClass;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;


public abstract class TestBase {
  protected Mongo mongo;
  protected DB db;
  protected Datastore ds;
  protected AdvancedDatastore ads;
  protected final Morphia morphia = new Morphia();

  protected TestBase() {
    try {
      mongo = new MongoClient(new MongoClientURI(System.getProperty("MONGO_URI", "mongodb://localhost:27017")));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    db = mongo.getDB("morphia_test");
    ds = morphia.createDatastore(mongo, db.getName());
    ads = (AdvancedDatastore) ds;
  }

  protected void cleanup() {
    for (final MappedClass mc : morphia.getMapper().getMappedClasses()) {
      db.getCollection(mc.getCollectionName()).drop();
    }

  }

  @After
  public void tearDown() {
    cleanup();
    mongo.close();
  }
}
