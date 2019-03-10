package dev.morphia.issue646;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;

@Converters(TriangleConverter.class)
public class SubClass extends BaseClass {

    @Embedded
    private Triangle triangle;

}
