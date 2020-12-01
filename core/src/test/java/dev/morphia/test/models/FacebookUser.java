package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

@Entity("facebook_users")
public class FacebookUser {
    @Id
    public long id;
    @Reference
    public final List<FacebookUser> friends = new ArrayList<>();
    public int loginCount;
    public String username;

    public FacebookUser(long id, String name) {
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
