package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Validation;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Entity("users")
@Validation("{ age : { $gte : 13 } }")
public class User {
    @Id
    private ObjectId id;
    public String name;
    public List<String> likes;
    public int age;
    private LocalDate joined;

    private User() {
    }

    public User(String name, LocalDate joined, String... likes) {
        this.name = name;
        this.joined = joined;
        this.likes = asList(likes);
    }

    @Override
    public String toString() {
        return format("User{id=%s, name='%s', joined=%s, likes=%s}", id, name, joined, likes);
    }
}
