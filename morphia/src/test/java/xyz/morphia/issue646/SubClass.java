package xyz.morphia.issue646;

import xyz.morphia.annotations.Converters;
import xyz.morphia.annotations.Embedded;

@Converters(TriangleConverter.class)
public class SubClass extends BaseClass {

    @Embedded
    private Triangle triangle;

}
