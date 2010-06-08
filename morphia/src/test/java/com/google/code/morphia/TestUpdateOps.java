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

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateResults;
import com.google.code.morphia.testmodel.Circle;
import com.google.code.morphia.testmodel.Rectangle;
import com.google.code.morphia.testutil.StandardTests;

/**
 *
 * @author Scott Hernandez
 */
public class TestUpdateOps  extends TestBase {


	public static class ContainsInt{
		protected @Id String id;
		public int val;
	}
	@Test @Category(StandardTests.class)
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
    public void testInsertUpdates() throws Exception {
		UpdateResults<Circle> res = ds.update(ds.createQuery(Circle.class).field("radius").equal(0), ds.createUpdateOperation().inc("radius",1D), true);
		assertEquals(1, res.getInsertedCount());
		assertEquals(0, res.getUpdatedCount());
		
		assertEquals(false, res.getUpdatedExisting());
	}

	@Test
    public void testUpdateTypeChange() throws Exception {
		ContainsInt cInt = new ContainsInt();
		cInt.val = 21;
		ds.save(cInt);
		
		UpdateResults<ContainsInt> res = ds.updateFirst(ds.createQuery(ContainsInt.class), ds.createUpdateOperation().inc("val",1.1D));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());		
		assertEquals(true, res.getUpdatedExisting());
		
		ContainsInt ciLoaded = ds.find(ContainsInt.class).limit(1).get();
		assertEquals(22, ciLoaded.val);
	}
	
	@Test
    public void testExistingUpdates() throws Exception {
		Circle c  = new Circle(100D);
		ds.save(c);
		c = new Circle(12D);
		ds.save(c);
		UpdateResults<Circle> res = ds.updateFirst(ds.createQuery(Circle.class), ds.createUpdateOperation().inc("radius",1D));
		assertEquals(1, res.getUpdatedCount());
		assertEquals(0, res.getInsertedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		res = ds.update(ds.createQuery(Circle.class), ds.createUpdateOperation().inc("radius"));
		assertEquals(2, res.getUpdatedCount());
		assertEquals(0, res.getInsertedCount());
		assertEquals(true, res.getUpdatedExisting());
	
		//test possible datatype change.
		Circle cLoaded = ds.find(Circle.class, "radius", 13).get();
		assertNotNull(cLoaded);		
		assertEquals(13D, cLoaded.getRadius(), 0D);
	}
}
