package dev.morphia.benchmarks.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

/**
 * An embedded value type.
 */
@Entity
public class Address {
    @Property("street_name")
    private String street;
    private String city;
    private String postCode;
    private String country;

    public Address() {
    }

    public Address(String street, String city, String postCode, String country) {
        this.street = street;
        this.city = city;
        this.postCode = postCode;
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }
}
