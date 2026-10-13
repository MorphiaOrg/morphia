package dev.morphia.test.mapping.codec.harness.models;

import java.util.List;

import dev.morphia.annotations.Entity;

@Entity
public class HarnessProfile {
    private String bio;
    private HarnessAddress address;
    private List<String> interests;

    public HarnessProfile() {
    }

    public HarnessProfile(String bio, HarnessAddress address, List<String> interests) {
        this.bio = bio;
        this.address = address;
        this.interests = interests;
    }
}
