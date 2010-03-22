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

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.testmodel.Address;
import com.google.code.morphia.testmodel.Hotel;
import com.google.code.morphia.testmodel.Rectangle;
import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 *
 * @author Scott Hernandez
 */
public class TestDatastore {

	Mongo mongo;
	Morphia morphia = new Morphia();
	DB db;
	Datastore ds;

	@Entity("facebook_users")
	public static class FacebookUser {
		@Id long id;
		String username;
		public FacebookUser() {}
		public FacebookUser(long id, String name) {
			this(); this.id = id; this.username = name;
		}
	}
	
	
	public TestDatastore () {
		try {
			mongo = new Mongo();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		morphia.map(Hotel.class).map(Rectangle.class).map(FacebookUser.class);
		//delete, and (re)create test db
	}

	@Before
	public void setUp() {
		mongo.dropDatabase("morphia_test");
		db = mongo.getDB("morphia_test");
        ds = morphia.createDatastore(mongo, db.getName());
        
	}
	
	@Test
    public void testNonexistantGet() throws Exception {
		assertNull(ds.get(Hotel.class, -1));
	}

	@Test
    public void testNonexistantFindGet() throws Exception {
		assertNull(ds.find(Hotel.class,"_id", -1).get());
	}
	
	@Test
    public void testCollectionNames() throws Exception {
		assertEquals("facebook_users", morphia.getMapper().getCollectionName(FacebookUser.class));
	}
	@Test
    public void testGet() throws Exception {
		List<FacebookUser> fbUsers = new ArrayList<FacebookUser>();
		fbUsers.add(new FacebookUser(1,"user 1"));
		fbUsers.add(new FacebookUser(2,"user 2"));
		fbUsers.add(new FacebookUser(3,"user 3"));
		fbUsers.add(new FacebookUser(4,"user 4"));
		
	
		ds.save(fbUsers);
		assertEquals(4, ds.getCount(FacebookUser.class));
		assertNotNull(ds.get(fbUsers.get(0), 1));
		Iterator<FacebookUser> it = ds.<FacebookUser>get(fbUsers.get(0), new long[] {1,2}).iterator();
		assertNotNull(it.next());
		assertNotNull(it.next());
		assertTrue(!it.hasNext());
	}
	
	@Test
    public void testSaveAndDelete() throws Exception {
		Rectangle rect = new Rectangle(10, 10);
		rect.setId("1");
		
		
		//test delete(entity)
		ds.save(rect);
		assertEquals(1, ds.getCount(rect));
		ds.delete(rect);
		assertEquals(0, ds.getCount(rect));

		//test delete(entity, id)
		ds.save(rect);
		assertEquals(1, ds.getCount(rect));
		ds.delete(rect, 1);
		assertEquals(1, ds.getCount(rect));
		ds.delete(rect, "1");
		assertEquals(0, ds.getCount(rect));

		//test delete(entity, {id})
		ds.save(rect);
		assertEquals(1, ds.getCount(rect));
		ds.delete(rect, new String[]{"1"});
		assertEquals(0, ds.getCount(rect));

		//test delete(entity, {id,id})
		rect.setId("1");
		ds.save(rect);
		rect.setId("2");
		ds.save(rect);
		assertEquals(2, ds.getCount(rect));
		ds.delete(rect, new String[]{"1", "2"});
		assertEquals(0, ds.getCount(rect));

		//test delete(entity, {id}) with one left
		rect.setId("1");
		ds.save(rect);
		rect.setId("2");
		ds.save(rect);
		assertEquals(2, ds.getCount(rect));
		ds.delete(rect, new String[]{"1"});
		assertEquals(1, ds.getCount(rect));

		//test delete(Class, {id}) with one left
		rect.setId("1");
		ds.save(rect);
		rect.setId("2");
		ds.save(rect);
		assertEquals(2, ds.getCount(rect));
		ds.delete(Rectangle.class, new String[]{"1"});
		assertEquals(1, ds.getCount(rect));
	}
	
    @Test
    public void testEmbedded() throws Exception {
        Hotel borg = Hotel.create();
        borg.setName("Hotel Borg");
        borg.setStars(4);
        borg.setTakesCreditCards(true);
        borg.setStartDate(new Date());
        borg.setType(Hotel.Type.LEISURE);
        Address borgAddr = new Address();
        borgAddr.setStreet("Posthusstraeti 11");
        borgAddr.setPostCode("101");
        borg.setAddress(borgAddr);

        
        ds.save(borg);
        assertEquals(1, ds.getCount(Hotel.class));
        assertNotNull(borg.getId());
        assertNotNull(borg.getCollectionName());

        Hotel hotelLoaded = ds.get(Hotel.class, borg.getId());
        assertEquals(borg.getName(), hotelLoaded.getName());
        assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());
        
    }
}
