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

import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

/**
 *
 * @author Scott Hernandez
 */
public class TestInheritanceMappings  extends TestBase {

	public static enum VehicleClass {
		Bicycle, Moped, MiniCar, Car, Truck;
	}
	public static interface Vehicle {
		String getId();
		int getWheelCount();
		VehicleClass getVehicleClass();
	}
	
	@Entity("vehicles")
	public static abstract class AbstractVehicle implements Vehicle{
		@Id String id;
		public String getId() {
			return this.id;
		}
	}
	
	public static class Car extends AbstractVehicle{
		public VehicleClass getVehicleClass() {
			return VehicleClass.Car;
		}

		public int getWheelCount() {
			return 4;
		}
	}

	public static class FlyingCar extends AbstractVehicle{
		public VehicleClass getVehicleClass() {
			return VehicleClass.Car;
		}

		public int getWheelCount() {
			return 0;
		}
	}
	

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
    
}
