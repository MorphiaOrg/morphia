package dev.morphia.critter.it.gen;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;

@Entity("hotels")
public class Hotel {
    @Id
    private String id;
    private String name;
    private int stars;
    private List<String> tags;
}
