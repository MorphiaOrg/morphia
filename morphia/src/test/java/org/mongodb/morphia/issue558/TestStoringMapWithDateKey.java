package org.mongodb.morphia.issue558;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.testutil.TestEntity;

public class TestStoringMapWithDateKey extends TestBase {

	@Override
	public void setUp() {
		super.setUp();
		getMorphia().map(User.class);
	}

	@Test
	public void testSaveFindEntity() {
		final User expectedUser = new User();
		expectedUser.addValue(new Date(), Double.valueOf(10d));
		getDs().save(expectedUser);
		
		final User actualUser = getDs().find(User.class).get();
		Assert.assertNotNull(actualUser.getUserMap());
	}

}

@Entity
class User extends TestEntity {
	private static final long serialVersionUID = -7755822989698927520L;
	private Map<Date, Double> userMap = new HashMap<Date, Double>();

	public Map<Date, Double> getUserMap() {
		return userMap;
	}

	public void addValue(Date date, Double value) {
		userMap.put(date, value);
	}

}