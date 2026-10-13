package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

@Entity
public class HarnessAddress {
    private String street;
    private String city;

    public HarnessAddress() {
    }

    public HarnessAddress(String street, String city) {
        this.street = street;
        this.city = city;
    }
}
