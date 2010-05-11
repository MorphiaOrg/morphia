/**
 * 
 */
package com.google.code.morphia.issue45;

import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public abstract class AbstractMorphiaTest
{
    protected Mongo mongo;
    protected DB db;
    protected Datastore ds;
    protected Morphia morphia;

    @Before
    public void setUp() throws UnknownHostException, MongoException
    {
        this.mongo = new Mongo();
        this.mongo.dropDatabase("morphia_test");
        this.db = this.mongo.getDB("morphia_test");
        this.morphia = new Morphia();
        this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
    }

    @After
    public void tearDown()
    {
        this.mongo.dropDatabase("morphia_test");
    }
}
