package com.google.code.morphia.mapping;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.UpdateRetainsClassInfoTest.X.E1;
import com.google.code.morphia.mapping.UpdateRetainsClassInfoTest.X.E2;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.mongodb.DB;
import com.mongodb.Mongo;

public class UpdateRetainsClassInfoTest extends TestBase {
	public static class X {
		public static abstract class E {
			@Id
			ObjectId id;
		}

		public static class E1 extends E {
			String foo;
		}

		public static class E2 extends E {
			String bar;
		}

		Map<String, E> map = new HashMap<String, E>();

	}

	@Test 
	@Ignore //FIXME
	public void retainsClassName() {
		X x = new X();

		E1 e1 = new E1();
		e1.foo = "narf";
		x.map.put("k1", e1);

		E2 e2 = new E2();
		e2.bar = "narf";
		x.map.put("k2", e2);

		ds.save(x);

		Query<X> state_query = ds.createQuery(X.class);
		UpdateOperations<X> state_update = ds
				.createUpdateOperations(X.class);
		state_update.set("map.k2", e2);

		ds.update(state_query, state_update);

		// fails due to type now missing
		x = ds.find(X.class).get();
	}
}