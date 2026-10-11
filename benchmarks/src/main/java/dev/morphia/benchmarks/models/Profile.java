package dev.morphia.benchmarks.models;

import java.util.List;

import dev.morphia.annotations.Entity;

/**
 * A second level of embedding, itself embedding an {@link Address}.
 */
@Entity
public class Profile {
    private String bio;
    private Address address;
    private List<String> interests;

    public Profile() {
    }

    public Profile(String bio, Address address, List<String> interests) {
        this.bio = bio;
        this.address = address;
        this.interests = interests;
    }

    public Address getAddress() {
        return address;
    }
}
