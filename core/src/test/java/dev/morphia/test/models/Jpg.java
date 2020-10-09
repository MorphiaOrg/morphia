package dev.morphia.test.models;

import dev.morphia.annotations.Entity;

import static dev.morphia.test.models.ImageType.JPG;

@Entity(value = "blogImages", discriminatorKey = "type", discriminator = "JPG")
public class Jpg extends BlogImage {

    public Jpg() {
        super(JPG);
    }
}
