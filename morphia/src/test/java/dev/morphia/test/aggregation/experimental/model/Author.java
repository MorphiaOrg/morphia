package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

import java.util.List;

@Entity("authors")
public class Author {
    @Id
    private String name;
    @Indexed
    private List<String> books;

    public String getName() {
        return name;
    }

    public List<String> getBooks() {
        return books;
    }
}
