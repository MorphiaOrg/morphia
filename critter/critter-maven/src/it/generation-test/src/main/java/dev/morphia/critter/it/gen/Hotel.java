package dev.morphia.critter.it.gen;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;
import java.util.Objects;

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
    public boolean equals(Object o) {
        if (!(o instanceof Hotel hotel)) {
            return false;
        }
        return stars == hotel.stars &&
               Objects.equals(id, hotel.id) &&
               Objects.equals(name, hotel.name) &&
               Objects.equals(tags, hotel.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, stars, tags);
    }
}
