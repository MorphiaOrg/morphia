package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import java.time.LocalDate;

import static java.time.LocalDate.now;

@Entity("banned")
public class BannedUser extends User {
    public BannedUser() {
        super("", now());
    }

    public BannedUser(final String name, final LocalDate joined, final String... likes) {
        super(name, joined, likes);
    }
}
