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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

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

	public static class ContainsIntArray{
		protected @Id String id;
		public Integer[] vals = {1,2,3};
	}

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
		
		UpdateResults<Rectangle> results = ds.update(q1, ds.createUpdateOperations().inc("height"));
		assertEquals(results.getUpdatedCount(), 3);
		assertEquals(results.getUpdatedExisting(), true);
		
		assertEquals(0, ds.getCount(q1));
		assertEquals(3, ds.getCount(q2));

		ds.update(q2, ds.createUpdateOperations().dec("height"));
		assertEquals(3, ds.getCount(q1));
		assertEquals(0, ds.getCount(q2));

		ds.update(ds.find(Rectangle.class, "width", 1D), ds.createUpdateOperations().add("height",1D).add("width", 1D), true);		
		assertNotNull(ds.find(Rectangle.class, "width", 1D).get());
		assertNull(ds.find(Rectangle.class, "width", 2D).get());
		ds.update(ds.find(Rectangle.class, "width", 1D), ds.createUpdateOperations().add("height",2D).add("width", 2D), true);		
		assertNull(ds.find(Rectangle.class, "width", 1D).get());
		assertNotNull(ds.find(Rectangle.class, "width", 2D).get());
	}
	
	@Test
    public void testInsertUpdates() throws Exception {
		UpdateResults<Circle> res = ds.update(ds.createQuery(Circle.class).field("radius").equal(0), ds.createUpdateOperations().inc("radius",1D), true);
		assertEquals(1, res.getInsertedCount());
		assertEquals(0, res.getUpdatedCount());
		
		assertEquals(false, res.getUpdatedExisting());
	}

	@Test
    public void testUpdateTypeChange() throws Exception {
		ContainsInt cInt = new ContainsInt();
		cInt.val = 21;
		ds.save(cInt);
		
		UpdateResults<ContainsInt> res = ds.updateFirst(ds.createQuery(ContainsInt.class), ds.createUpdateOperations().inc("val",1.1D));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());		
		assertEquals(true, res.getUpdatedExisting());
		
		ContainsInt ciLoaded = ds.find(ContainsInt.class).limit(1).get();
		assertEquals(22, ciLoaded.val);
	}

	@Test
    public void testAdd() throws Exception {
		ContainsIntArray cIntArray = new ContainsIntArray();
		ds.save(cIntArray);
		ContainsIntArray cIALoaded = ds.get(cIntArray);
		assertEquals(3, cIALoaded.vals.length);
		assertArrayEquals((new ContainsIntArray()).vals, cIALoaded.vals);
		
		//add 4 to array
		UpdateResults<ContainsIntArray > res = ds.updateFirst(ds.createQuery(ContainsIntArray .class), ds.createUpdateOperations().add("vals",4, false));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		cIALoaded = ds.get(cIntArray);
		assertArrayEquals(new Integer[]{1,2,3,4}, cIALoaded.vals);

		//add unique (4) -- noop
		res = ds.updateFirst(ds.createQuery(ContainsIntArray .class), ds.createUpdateOperations().add("vals",4, false));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		cIALoaded = ds.get(cIntArray);
		assertArrayEquals(new Integer[]{1,2,3,4}, cIALoaded.vals);

		//add dup 4
		res = ds.updateFirst(ds.createQuery(ContainsIntArray .class), ds.createUpdateOperations().add("vals",4, true));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		cIALoaded = ds.get(cIntArray);
		assertArrayEquals(new Integer[]{1,2,3,4,4}, cIALoaded.vals);

		//cleanup for next tests
		ds.delete(ds.find(ContainsIntArray.class));
		cIntArray = ds.getByKey(ContainsIntArray.class, ds.save(new ContainsIntArray()));
		
		//add [4,5]
		List<Integer> newVals = new ArrayList<Integer>();
		newVals.add(4);newVals.add(5);
		res = ds.updateFirst(ds.createQuery(ContainsIntArray .class), ds.createUpdateOperations().addAll("vals", newVals, false));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		cIALoaded = ds.get(cIntArray);
		assertArrayEquals(new Integer[]{1,2,3,4,5}, cIALoaded.vals);
		
		//add them again... noop
		res = ds.updateFirst(ds.createQuery(ContainsIntArray .class), ds.createUpdateOperations().addAll("vals", newVals, false));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		cIALoaded = ds.get(cIntArray);
		assertArrayEquals(new Integer[]{1,2,3,4,5}, cIALoaded.vals);

		//add dups [4,5]
		res = ds.updateFirst(ds.createQuery(ContainsIntArray .class), ds.createUpdateOperations().addAll("vals", newVals, true));
		assertEquals(0, res.getInsertedCount());
		assertEquals(1, res.getUpdatedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		cIALoaded = ds.get(cIntArray);
		assertArrayEquals(new Integer[]{1,2,3,4,5,4,5}, cIALoaded.vals);

	}
	
	@Test
    public void testExistingUpdates() throws Exception {
		Circle c  = new Circle(100D);
		ds.save(c);
		c = new Circle(12D);
		ds.save(c);
		UpdateResults<Circle> res = ds.updateFirst(ds.createQuery(Circle.class), ds.createUpdateOperations().inc("radius",1D));
		assertEquals(1, res.getUpdatedCount());
		assertEquals(0, res.getInsertedCount());
		assertEquals(true, res.getUpdatedExisting());
		
		res = ds.update(ds.createQuery(Circle.class), ds.createUpdateOperations().inc("radius"));
		assertEquals(2, res.getUpdatedCount());
		assertEquals(0, res.getInsertedCount());
		assertEquals(true, res.getUpdatedExisting());
	
		//test possible datatype change.
		Circle cLoaded = ds.find(Circle.class, "radius", 13).get();
		assertNotNull(cLoaded);		
		assertEquals(13D, cLoaded.getRadius(), 0D);
	}
}
