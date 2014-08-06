package org.mongodb.morphia.issue646;

import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;

@Converters(TriangleConverter.class)
public class SubClass extends BaseClass {

    @Embedded
    private Triangle triangle;

}
