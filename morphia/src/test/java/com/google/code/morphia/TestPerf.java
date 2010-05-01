/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia;

import java.net.UnknownHostException;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 *
 * @author Scott Hernandez
 */
@Ignore
public class TestPerf {

	Mongo mongo;
	Morphia morphia = new Morphia();
	DB db;
	Datastore ds;

	@Entity
	public static class Address {
		@Id String id;
		String name = "Scott";
		String street = "3400 Maple";
		String city = "Manhattan Beach";
		String state = "CA";
		int zip = 94114;
	}
	
	public TestPerf () {
		try {
			mongo = new Mongo();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		morphia.map(Address.class);
	}

	@Before
	public void setUp() {
		mongo.dropDatabase("morphia_test");
		db = mongo.getDB("morphia_test");
        ds = morphia.createDatastore(mongo, db.getName());
	}

    @Test
    public void testAddressInsertPerf() throws Exception {
    	int count = 10000;
    	long startTicks = new Date().getTime();
    	insertAddresses(count, true);
    	long endTicks = new Date().getTime();
    	long rawInsertTime = endTicks - startTicks;
    	
    	ds.delete(ds.find(Address.class));
    	startTicks = new Date().getTime();
    	insertAddresses(count, false);
    	endTicks = new Date().getTime();
    	long insertTime = endTicks - startTicks;
    	
    	Assert.assertTrue("Insert(" + count + " addresses) performance is too slow: " + 
    				String.valueOf((double)insertTime/rawInsertTime).subSequence(0, 5) + "X slower", 
    			insertTime < (rawInsertTime * 1.1));
    }
    
    public void insertAddresses(int count, boolean raw) {
    	Address template = new Address();
    	DBCollection dbColl = db.getCollection(((DatastoreImpl)ds).getMapper().getCollectionName(Address.class));
    	for(int i=0;i<count;i++) {
    		if(raw) {
    			DBObject addr = new BasicDBObject();
    			addr.put("name", template.name);
    			addr.put("street", template.street);
    			addr.put("city", template.city);
    			addr.put("state", template.state);
    			addr.put("zip", template.zip);
    			dbColl.save(addr);
    		}else {
    			Address addr = new Address();
    			ds.save(addr);
    		}
    	}
    }
}
