package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

@Entity("facebook_users")
public class FacebookUser {
    @Reference
    public final List<FacebookUser> friends = new ArrayList<>();
    @Id
    public long id;
    public int loginCount;
    public String username;

    public FacebookUser(final long id, final String name) {
        this();
        this.id = id;
        username = name;
    }

    public FacebookUser() {
    }

    public long getId() {
        return id;
    }
}
