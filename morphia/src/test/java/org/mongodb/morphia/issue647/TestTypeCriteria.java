package org.mongodb.morphia.issue647;

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Type;

import com.mongodb.MongoClient;

public class TestTypeCriteria extends TestBase {

	@Entity(value = "user", noClassnameStored = true)
	public static class Class1 {
		@Id
		private ObjectId id;
		
		@Property("first_name")
		private String firstName;

		@Property("last_name")
		private String lastName;

		private boolean status;

		@Property("create_date")
		private long createDt;

		public ObjectId getId() {
			return id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public boolean isStatus() {
			return status;
		}

		public void setStatus(boolean status) {
			this.status = status;
		}

		public long getCreateDt() {
			return createDt;
		}

		public void setCreateDt(long createDt) {
			this.createDt = createDt;
		}

		public void setId(ObjectId id) {
			this.id = id;
		}

	}


	@Test
	public void getStringTypeData() {
		getMorphia().map(Class1.class);

		Morphia morphia =  getMorphia().map(Class1.class);
		try {
			MongoClient mongoClient = new MongoClient("localhost");
			Datastore ds = morphia.createDatastore( mongoClient, "test" );

			Query<Class1> query = ds.createQuery(Class1.class);
			query.criteria("first_name").type(Type.STRING);
			Assert.assertTrue(query.asList().size()>0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Field is string so it will return 0 data
	 */
	@Test
	public void getArrayTypeData() {
		getMorphia().map(Class1.class);

		Morphia morphia =  getMorphia().map(Class1.class);
		try {
			MongoClient mongoClient = new MongoClient("localhost");
			Datastore ds = morphia.createDatastore( mongoClient, "test" );

			Query<Class1> query = ds.createQuery(Class1.class);
			query.criteria("first_name").type(Type.DOUBLE);
			Assert.assertTrue(query.asList().size()>0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
