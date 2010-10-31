/**
 * 
 */
package com.google.code.morphia;

import org.junit.After;
import org.junit.Before;

import com.mongodb.DB;
import com.mongodb.Mongo;

public abstract class TestBase
{
    protected Mongo mongo;
    protected DB db;
    protected Datastore ds;
    protected Morphia morphia = new Morphia();

    protected TestBase() {
        try {
			this.mongo = new Mongo();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    @Before
    public void setUp()
    {
    	dropDB();
        this.db = this.mongo.getDB("morphia_test");
        this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
    }
	
    protected void dropDB() {
        this.mongo.dropDatabase("morphia_test");
    	
    }
    
	@After
	public void tearDown() {
//		new ScopedFirstLevelCacheProvider().release();
	}
}
