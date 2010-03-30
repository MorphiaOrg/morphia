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

import java.net.UnknownHostException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.testmodel.Rectangle;
import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 *
 * @author Scott Hernandez
 */
public class TestSuperDatastore {

	Mongo mongo;
	Morphia morphia = new Morphia();
	DB db;
	SuperDatastore ds;
	
	public TestSuperDatastore () {
		try {
			mongo = new Mongo();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		morphia.map(Rectangle.class);
		//delete, and (re)create test db
	}

	@Before
	public void setUp() {
		mongo.dropDatabase("morphia_test");
		db = mongo.getDB("morphia_test");
        ds = (SuperDatastore)morphia.createDatastore(mongo, db.getName());
	}

	@Test
    public void testSaveAndDelete() throws Exception {
		String ns = "hotels";
		Rectangle rect = new Rectangle(10, 10);
		rect.setId("1");
		
		
		//test delete(entity, id)
		ds.save(ns, rect);
		assertEquals(1, ds.getCount(ns));
		ds.delete(ns, 1);
		assertEquals(1, ds.getCount(ns));
		ds.delete(ns, "1");
		assertEquals(0, ds.getCount(ns));
	}
	
	@Test
    public void testGet() throws Exception {
		String ns = "hotels";
		Rectangle rect = new Rectangle(10, 10);
		rect.setId("1");
		
		
		//test delete(entity, id)
		ds.save(ns, rect);
		assertEquals(1, ds.getCount(ns));
		Rectangle rectLoaded = ds.get(ns, Rectangle.class, rect.getId());
		assertEquals(rect.getId(), rectLoaded.getId());
		assertEquals(rect.getArea(), rectLoaded.getArea(), 0);
	}	

	@Test
    public void testFind() throws Exception {
		String ns = "hotels";
		Rectangle rect = new Rectangle(10, 10);
		rect.setId("1");
		
		//test delete(entity, id)
		ds.save(ns, rect);
		assertEquals(1, ds.getCount(ns));
		Rectangle rectLoaded = ds.find(ns, Rectangle.class).get();
		assertEquals(rect.getId(), rectLoaded.getId());
		assertEquals(rect.getArea(), rectLoaded.getArea(), 0);
		
		rect = new Rectangle(2, 1);
		rect.setId("2");
		ds.save(rect); //saved to default collection name (kind)
		assertEquals(1, ds.getCount(rect));
		
		rect.setId("3");
		ds.save(rect); //saved to default collection name (kind)
		assertEquals(2, ds.getCount(rect));
		
		rect = new Rectangle(4, 3);
		rect.setId("3");
		ds.save(ns, rect);
		assertEquals(2, ds.getCount(ns));
		List<Rectangle> rects = ds.find(ns, Rectangle.class).asList();
		
		rectLoaded = rects.get(1);
		assertEquals(rect.getId(), rectLoaded.getId());
		assertEquals(rect.getArea(), rectLoaded.getArea(), 0);
		
		rectLoaded = ds.find(ns, Rectangle.class, "_id !=", "-1", 1, 1).get();	
		
	}	
}
