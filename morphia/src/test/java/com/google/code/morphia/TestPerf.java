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

import java.util.Date;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.mongodb.BasicDBObject;
import com.mongodb.DB.WriteConcern;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 *
 * @author Scott Hernandez
 */
public class TestPerf  extends TestBase{
	static double FailFactor = 1.05;
	
	@Entity
	public static class Address {
		@Id String id;
		String name = "Scott";
		String street = "3400 Maple";
		String city = "Manhattan Beach";
		String state = "CA";
		int zip = 94114;
		Date added = new Date();
	}

	@Test @Ignore
    public void testAddressInsertPerf() throws Exception {
    	int count = 5000;
    	boolean strict = true;
    	long startTicks = new Date().getTime();
    	insertAddresses(count, true, strict);
    	long endTicks = new Date().getTime();
    	long rawInsertTime = endTicks - startTicks;
    	
    	ds.delete(ds.find(Address.class));
    	startTicks = new Date().getTime();
    	insertAddresses(count, false, strict);
    	endTicks = new Date().getTime();
    	long insertTime = endTicks - startTicks;
    	
    	String msg = String.format("Insert (%s) performance is too slow: %sX slower (%s/%s)", 
    							count,
    							String.valueOf((double)insertTime/rawInsertTime).subSequence(0, 4),
    							insertTime,
    							rawInsertTime);
    	Assert.assertTrue(msg, 
    			insertTime < (rawInsertTime * FailFactor ));
    }

	@Test @Ignore
    public void testAddressLoadPerf() throws Exception {
    	insertAddresses(10, false, false);
    	
		int count = 5000;
    	boolean strict = true;
    	long startTicks = new Date().getTime();
    	loadAddresses(count, true, strict);
    	long endTicks = new Date().getTime();
    	long rawInsertTime = endTicks - startTicks;
    	
    	startTicks = new Date().getTime();
    	loadAddresses(count, false, strict);
    	endTicks = new Date().getTime();
    	long insertTime = endTicks - startTicks;
    	
    	String msg = String.format("Load (%s) performance is too slow: %sX slower (%s/%s)", 
    							count,
    							String.valueOf((double)insertTime/rawInsertTime).subSequence(0, 4),
    							insertTime,
    							rawInsertTime);
    	Assert.assertTrue(msg, 
    			insertTime < (rawInsertTime * FailFactor ));
    }
	
	public void loadAddresses(int count, boolean raw, boolean strict) {
    	DBCollection dbColl = db.getCollection(((DatastoreImpl)ds).getMapper().getCollectionName(Address.class));
    	if (strict) 
    		dbColl.setWriteConcern(WriteConcern.STRICT);
    	
    	for(int i=0;i<count;i++) {
    		if(raw) {
    			Address addr = new Address();
    			BasicDBObject dbObj = (BasicDBObject) dbColl.findOne();
    			addr.name = dbObj.getString("name");
    			addr.street = dbObj.getString("street");
    			addr.city = dbObj.getString("city");
    			addr.state = dbObj.getString("state");
    			addr.zip = dbObj.getInt("zip");
    			addr.added = (Date) dbObj.get("added");
    			dbColl.save(dbObj);
    		}else {
    			ds.find(Address.class).get();
    		}
    	}
    }

	public void insertAddresses(int count, boolean raw, boolean strict) {
    	DBCollection dbColl = db.getCollection(((DatastoreImpl)ds).getMapper().getCollectionName(Address.class));
    	if (strict) 
    		dbColl.setWriteConcern(WriteConcern.STRICT);
    	
    	for(int i=0;i<count;i++) {
			Address addr = new Address();
    		if(raw) {
    			DBObject dbObj = new BasicDBObject();
    			dbObj.put("name", addr.name);
    			dbObj.put("street", addr.street);
    			dbObj.put("city", addr.city);
    			dbObj.put("state", addr.state);
    			dbObj.put("zip", addr.zip);
    			dbObj.put("added", new Date());
    			dbColl.save(dbObj);
    		}else {
    			ds.save(addr);
    		}
    	}
    }
}
