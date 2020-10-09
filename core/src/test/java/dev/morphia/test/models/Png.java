package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import static dev.morphia.test.models.ImageType.PNG;

@Entity(value = "blogImages", discriminatorKey = "type", discriminator = "PNG")
public class Png extends BlogImage {
    public Png() {
        super(PNG);
    }
}
