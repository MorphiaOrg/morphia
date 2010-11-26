package com.google.code.morphia.query;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.utils.IndexDirection;
@Ignore
public class TestGeoQueries extends TestBase {
	@Entity
	@SuppressWarnings("unused")
	static private class Place {
		@Id protected ObjectId id;
		protected String name = "";
		@Indexed(IndexDirection.GEO2D)
		protected double[] loc = null;
		
		public Place(String name, double[] loc) {
			this.name = name;
			this.loc = loc;
		}
		private Place() {}
	}
	
	
	@Override
	public void setUp() {
		super.setUp();
		morphia.map(Place.class);
	}
	
	@Test
	public void testNear() throws Exception {
		ds.ensureIndexes();
		Place place1 = new Place("place1", new double[] {1,1});
		ds.save(place1);
		Place found = ds.find(Place.class).field("loc").near(0, 0).get();
		Assert.assertNotNull(found);
	}
	
	@Test 
	public void testNearNoIndex() throws Exception {
		Place place1 = new Place("place1", new double[] {1,1});
		ds.save(place1);
		Place found = ds.find(Place.class).field("loc").near(0, 0).get();
		Assert.assertNotNull(found);
	}

	@Test
	public void testNearWithRadius() throws Exception {
		ds.ensureIndexes();
		Place place1 = new Place("place1", new double[] {1,1});
		ds.save(place1);
		Place found = ds.find(Place.class).field("loc").near(0, 0, 1.1).get();
		Assert.assertNotNull(found);
	}

}
