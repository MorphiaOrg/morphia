package dev.morphia.critter.sources;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

@Entity
public class Example {
    @Property("myName")
    @AlsoLoad({ "name1", "name2" })
    private String name;
}
