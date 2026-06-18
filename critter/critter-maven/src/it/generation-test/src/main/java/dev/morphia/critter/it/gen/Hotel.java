package dev.morphia.critter.it.gen;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;
import java.util.Objects;

import com.mongodb.lang.NonNullApi;

@Entity("hotels")
public class Hotel {
    @Id
    private String id;
    private String name;
    private int stars;
    private List<String> tags;

    @SafeVarargs
    private String foo(String... bob) {
        return "";
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, stars, tags);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
