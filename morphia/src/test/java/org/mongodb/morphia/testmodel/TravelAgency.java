/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package org.mongodb.morphia.testmodel;


import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Entity("agencies")
public class TravelAgency extends TestEntity {
    @Property
    private String name;

    @Reference
    private List<Hotel> hotels;

    public TravelAgency() {
        hotels = new ArrayList<Hotel>();
    }

    public List<Hotel> getHotels() {
        return hotels;
    }

    public void setHotels(final List<Hotel> hotels) {
        this.hotels = hotels;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
