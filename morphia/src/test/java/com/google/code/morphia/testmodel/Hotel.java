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

package com.google.code.morphia.testmodel;

import com.google.code.morphia.AbstractMongoEntity;
import com.google.code.morphia.annotations.MongoEmbedded;
import com.google.code.morphia.annotations.MongoValue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Hotel extends AbstractMongoEntity {

    public enum Type { BUSINESS, LEISURE }

    @MongoValue
    private String name;

    @MongoValue
    private Date startDate;

    @MongoValue
    private int stars;

    @MongoValue
    private boolean takesCreditCards;

    @MongoValue
    private Type type;

    @MongoValue
    private List<String> tags;

    @MongoEmbedded
    private Address address;

    @MongoEmbedded(listClass = Vector.class)
    private List<PhoneNumber> phoneNumbers;

    public Hotel() {
        super();
        tags = new ArrayList<String>();
        phoneNumbers = new Vector<PhoneNumber>();
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public boolean isTakesCreditCards() {
        return takesCreditCards;
    }

    public void setTakesCreditCards(boolean takesCreditCards) {
        this.takesCreditCards = takesCreditCards;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
}
