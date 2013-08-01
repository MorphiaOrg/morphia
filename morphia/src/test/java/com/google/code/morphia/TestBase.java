package com.google.code.morphia;


import org.junit.After;
import org.junit.Before;

import com.google.code.morphia.mapping.MappedClass;
import com.mongodb.DB;
import com.mongodb.MongoClient;


public abstract class TestBase {
  protected MongoClient             mongo;
  protected DB                db;
  protected Datastore         ds;
  protected AdvancedDatastore ads;
  protected final Morphia morphia = new Morphia();

  protected TestBase() {
    try {
      mongo = new MongoClient();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    db = mongo.getDB("morphia_test");
    ds = morphia.createDatastore(mongo, db.getName());
    ads = (AdvancedDatastore) ds;
    //ads.setDecoderFact(LazyDBDecoder.FACTORY);
  }

  protected void cleanup() {
    //this.mongo.dropDatabase("morphia_test");
    for (final MappedClass mc : morphia.getMapper().getMappedClasses())
    //			if( mc.getEntityAnnotation() != null )
    {
      db.getCollection(mc.getCollectionName()).drop();
    }

  }

  @After
  public void tearDown() {
    cleanup();
    mongo.close();
  }
}
