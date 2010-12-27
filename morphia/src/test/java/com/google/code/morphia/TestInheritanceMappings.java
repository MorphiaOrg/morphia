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

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 *
 * @author Scott Hernandez
 */
public class TestInheritanceMappings extends TestBase {

	
	private static enum VehicleClass {
		Bicycle, Moped, MiniCar, Car, Truck;
	}
	private static interface Vehicle {
		String getId();
		int getWheelCount();
		VehicleClass getVehicleClass();
	}
	
	@Entity("vehicles")
	private static abstract class AbstractVehicle implements Vehicle{
		@Id ObjectId id;
		public String getId() {
			return id.toString();
		}
	}
	
	private static class Car extends AbstractVehicle{
		public VehicleClass getVehicleClass() {
			return VehicleClass.Car;
		}

		public int getWheelCount() {
			return 4;
		}
	}

	private static class FlyingCar extends AbstractVehicle{
		public VehicleClass getVehicleClass() {
			return VehicleClass.Car;
		}

		public int getWheelCount() {
			return 0;
		}
	}

	private static class GenericIdPlus<T,K> {
		@Id T id;
		K k;
	}
	private static class ParameterizedEntity extends GenericIdPlus<String, Long>{
		String b;
	}
	
	private static class GenericId<T> {
		@Id T id;
	}
	
	private static class GenericIdSub<V> extends GenericId<V> {}
	private static class ParameterizedIdEntity2 extends GenericIdSub<String> {}
	
	private static class ParameterizedIdEntity extends GenericId<String> {}

	@Before @Override
	public void setUp() {
		super.setUp();
		morphia.map(Car.class).map(AbstractVehicle.class).map(FlyingCar.class);
	}

    @Test
    public void testSuperclassEntity() throws Exception {
    	Car c = new Car();
    	ds.save(c);
    	assertNotNull(c.getId());
    	
    	assertEquals(ds.getCount(Car.class), 1);
    	assertEquals(ds.getCount(AbstractVehicle.class), 1);
    	
    }

    @Test
    public void testParamIdEntity() throws Exception {
    	morphia.map(ParameterizedIdEntity.class);
    	ParameterizedIdEntity c = new ParameterizedIdEntity();
    	c.id = "foo";
    	ds.save(c);
    	c = ds.get(ParameterizedIdEntity.class, "foo");
    	assertNotNull(c.id);
    	
    	assertEquals("foo", c.id);
    	assertEquals(ds.getCount(ParameterizedIdEntity.class), 1);
    }

    @Test
    public void testParamIdEntity2() throws Exception {
    	morphia.map(ParameterizedIdEntity2.class);
    	ParameterizedIdEntity2 c = new ParameterizedIdEntity2();
    	c.id = "foo";
    	ds.save(c);
    	c = ds.get(ParameterizedIdEntity2.class, "foo");
    	assertNotNull(c.id);
    	
    	assertEquals("foo", c.id);
    	assertEquals(ds.getCount(ParameterizedIdEntity2.class), 1);
    }
    
    @Test
    public void testParamEntity() throws Exception {
    	morphia.map(ParameterizedEntity.class);
    	ParameterizedEntity c = new ParameterizedEntity();
    	c.id = "foo";
    	c.b = "eh";
    	c.k = 12L;
    	ds.save(c);
    	c = ds.get(ParameterizedEntity.class, "foo");
    	assertNotNull(c.id);
    	assertNotNull(c.b);
    	assertNotNull(c.k);
    	
    	assertEquals("foo", c.id);
    	assertEquals("eh", c.b);
    	assertEquals(12, c.k.longValue());
    	assertEquals(ds.getCount(ParameterizedEntity.class), 1);
    }
    
}
