package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

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
    private Address address;
    @Property(concreteClass = Vector.class)
    private List<PhoneNumber> phoneNumbers;

    public Hotel() {
        tags = new HashSet<>();
        phoneNumbers = new Vector<>();
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

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isTakesCreditCards() {
        return takesCreditCards;
    }

    public void setTakesCreditCards(boolean takesCreditCards) {
        this.takesCreditCards = takesCreditCards;
    }

    public enum Type {
        BUSINESS,
        LEISURE
    }
}
