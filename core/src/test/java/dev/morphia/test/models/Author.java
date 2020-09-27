package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity("authors")
public class Author {
    @Id
    public String name;
}
