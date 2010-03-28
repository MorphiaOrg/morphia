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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.annotations.CappedAt;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 *
 * @author Scott Hernandez
 */
public class TestIndexedCapped {

	Mongo mongo;
	Morphia morphia = new Morphia();
	DB db;
	Datastore ds;

	@Entity(cap=@CappedAt(count=1))
	public static class CurrentStatus{
		@Id String id;
		String message;
		
		@SuppressWarnings("unused")
		private CurrentStatus() {}
		public CurrentStatus(String msg) {message = msg;}
	}

	@Entity
	public static class IndexedClass{
		@Id String id;
		@Indexed long l=4;
	}

	@Entity
	public static class NamedIndexClass{
		@Id String id;
		@Indexed(name="l_ascending") long l=4;	
	}
	
	public TestIndexedCapped () {
		try {
			mongo = new Mongo();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		morphia.map(CurrentStatus.class).map(IndexedClass.class).map(NamedIndexClass.class);
	}

	@Before
	public void setUp() {
		mongo.dropDatabase("morphia_test");
		db = mongo.getDB("morphia_test");
        ds = morphia.createDatastore(mongo, db.getName());
	}

	@Test
    public void testCappedEntity() throws Exception {
		ds.ensureCaps();
		CurrentStatus cs = new CurrentStatus("All Good");
		ds.save(cs);
		assertEquals(ds.getCount(CurrentStatus.class), 1);
		ds.save( new CurrentStatus("Kinda Bad"));
		assertEquals(ds.getCount(CurrentStatus.class), 1);
		assertTrue(ds.find(CurrentStatus.class).limit(1).get().message.contains("Bad"));
		ds.save( new CurrentStatus("Kinda Bad2"));
		assertEquals(ds.getCount(CurrentStatus.class), 1);
		ds.save( new CurrentStatus("Kinda Bad3"));
		assertEquals(ds.getCount(CurrentStatus.class), 1);
		ds.save( new CurrentStatus("Kinda Bad4"));
		assertEquals(ds.getCount(CurrentStatus.class), 1);
	}
	
	@Test
    public void testIndexedEntity() throws Exception {
		MappedClass mc = morphia.getMapper().getMappedClass(IndexedClass.class);
		ds.ensureIndexes();
		assertTrue(hasIndexedField("l",db.getCollection(mc.defCollName).getIndexInfo()));
		ds.save(new IndexedClass());
		ds.ensureIndexes();
		assertTrue(hasIndexedField("l",db.getCollection(mc.defCollName).getIndexInfo()));
	}
	
	@Test
    public void testNamedIndexEntity() throws Exception {
		MappedClass mc = morphia.getMapper().getMappedClass(NamedIndexClass.class);
		ds.ensureIndexes();
		assertTrue(hasIndexedField("l",db.getCollection(mc.defCollName).getIndexInfo()));
		ds.save(new IndexedClass());
		ds.ensureIndexes();
		assertTrue(hasIndexedField("l",db.getCollection(mc.defCollName).getIndexInfo()));
		
		assertTrue(hasNamedIndex("l_ascending", db.getCollection(mc.defCollName).getIndexInfo()));
	}

	protected boolean hasNamedIndex(String name, List<DBObject> indexes) {
		for(DBObject dbObj : indexes) {
			if (dbObj.get("name").equals(name)) return true;
		}
		return false;
	}

	protected boolean hasIndexedField(String name, List<DBObject> indexes) {
		for(DBObject dbObj : indexes) {
			if (((DBObject)dbObj.get("key")).containsField(name)) return true;
		}
		return false;
	}
}
