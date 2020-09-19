package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import java.time.LocalDate;

import static java.time.LocalDate.now;

@Entity("banned")
public class BannedUser extends User {
    public BannedUser() {
        super("", now());
    }

    public BannedUser(String name, LocalDate joined, String... likes) {
        super(name, joined, likes);
    }
}
