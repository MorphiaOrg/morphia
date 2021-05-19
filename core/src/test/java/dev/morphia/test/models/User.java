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
    public String name;
    public List<String> likes;
    public int age;
    @Id
    private ObjectId id;
    private LocalDate joined;

    public User() {
    }

    public User(String name, LocalDate joined, String... likes) {
        this.name = name;
        this.joined = joined;
        this.likes = asList(likes);
    }

    public User(String name, LocalDate joined) {
        this.name = name;
        this.joined = joined;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public LocalDate getJoined() {
        return joined;
    }

    public void setJoined(LocalDate joined) {
        this.joined = joined;
    }

    public List<String> getLikes() {
        return likes;
    }

    public void setLikes(List<String> likes) {
        this.likes = likes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return format("User{id=%s, name='%s', joined=%s, likes=%s}", id, name, joined, likes);
    }
}
