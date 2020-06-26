package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity(value = "budget", useDiscriminator = false)
public class Budget {
    @Id
    private int id;
    private String category;
    private int budget;
    private int spent;
}
