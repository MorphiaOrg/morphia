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


package dev.morphia.testmodel;


import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Transient;
import dev.morphia.testutil.TestEntity;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Entity("hotels")
public class Hotel extends TestEntity {
    private String name;
    private Date startDate;
    private int stars;
    private boolean takesCreditCards;
    private Type type;
    private Set<String> tags;
    @Transient
    private String temp;
    @Embedded
    private Address address;
    @Embedded(concreteClass = Vector.class)
    private List<PhoneNumber> phoneNumbers;

    public Hotel() {
        tags = new HashSet<String>();
        phoneNumbers = new Vector<PhoneNumber>();
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(final List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(final int stars) {
        this.stars = stars;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(final Set<String> tags) {
        this.tags = tags;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(final String temp) {
        this.temp = temp;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public boolean isTakesCreditCards() {
        return takesCreditCards;
    }

    public void setTakesCreditCards(final boolean takesCreditCards) {
        this.takesCreditCards = takesCreditCards;
    }

    public enum Type {
        BUSINESS,
        LEISURE
    }
}
