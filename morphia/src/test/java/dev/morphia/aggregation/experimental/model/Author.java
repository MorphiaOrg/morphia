package dev.morphia.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;

@Entity("authors")
public class Author {
    @Id
    private String name;
    private List<String> books;
}
