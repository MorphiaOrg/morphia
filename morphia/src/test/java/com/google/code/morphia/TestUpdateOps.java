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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateResults;
import com.google.code.morphia.testmodel.Circle;
import com.google.code.morphia.testmodel.Rectangle;
import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 *
 * @author Scott Hernandez
 */
public class TestUpdateOps {

	Mongo mongo;
	Morphia morphia = new Morphia();
	DB db;
	Datastore ds;

	public TestUpdateOps() {
		try {
			mongo = new Mongo();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setUp() {
		mongo.dropDatabase("morphia_test");
		db = mongo.getDB("morphia_test");
        ds = morphia.createDatastore(mongo, db.getName());
	}
	
	@Test
    public void testUpdateSingleField() throws Exception {
		Rectangle[] rects = {	new Rectangle(1, 10),
								new Rectangle(1, 10),
								new Rectangle(1, 10),
								new Rectangle(10, 10),
								new Rectangle(10, 10),
								};
		for(Rectangle rect: rects)
			ds.save(rect);
		
		Query<Rectangle> q1 = ds.find(Rectangle.class, "height", 1D);
		Query<Rectangle> q2 = ds.find(Rectangle.class, "height", 2D);
		
		assertEquals(3, ds.getCount(q1));
		assertEquals(0, ds.getCount(q2));
		
		UpdateResults<Rectangle> results = ds.update(q1, ds.createUpdateOperation().inc("height"));
		assertEquals(results.getUpdatedCount(), 3);
		assertEquals(results.getUpdatedExisting(), true);
		
		assertEquals(0, ds.getCount(q1));
		assertEquals(3, ds.getCount(q2));

		ds.update(q2, ds.createUpdateOperation().dec("height"));
		assertEquals(3, ds.getCount(q1));
		assertEquals(0, ds.getCount(q2));

		ds.update(ds.find(Rectangle.class, "width", 1D), ds.createUpdateOperation().add("height",1D).add("width", 1D), true);		
		assertNotNull(ds.find(Rectangle.class, "width", 1D).get());
		assertNull(ds.find(Rectangle.class, "width", 2D).get());
		ds.update(ds.find(Rectangle.class, "width", 1D), ds.createUpdateOperation().add("height",2D).add("width", 2D), true);		
		assertNull(ds.find(Rectangle.class, "width", 1D).get());
		assertNotNull(ds.find(Rectangle.class, "width", 2D).get());
	}

	@Test
    public void testSingleExistingUpdate() throws Exception {
		Circle c  = new Circle(100D);
		ds.save(c);
		c = new Circle(12D);
		ds.save(c);
		UpdateResults<Circle> res = ds.updateFirst(ds.createQuery(Circle.class), ds.createUpdateOperation().inc("radius",1D));
		assertEquals(1, res.getUpdatedCount());
		assertEquals(0, res.getInsertedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		res = ds.update(ds.createQuery(Circle.class), ds.createUpdateOperation().inc("radius",1D));
		assertEquals(2, res.getUpdatedCount());
		assertEquals(0, res.getInsertedCount());
		assertEquals(true, res.getUpdatedExisting());

		res = ds.update(ds.createQuery(Circle.class).field("radius").equal(0), ds.createUpdateOperation().inc("radius",1D), true);
		assertEquals(1, res.getInsertedCount());
		assertEquals(0, res.getUpdatedCount());
		
		assertEquals(false, res.getUpdatedExisting());

	}
    
}
