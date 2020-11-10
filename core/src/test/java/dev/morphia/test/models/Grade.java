package dev.morphia.test.models;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

import java.util.Map;

@Embedded
public class Grade {
    private int marks;

    @Property("d")
    private Map<String, String> data;

    public Grade() {
    }

    public Grade(int marks, Map<String, String> data) {
        this.marks = marks;
        this.data = data;
    }

    @Override
    public String toString() {
        return ("marks: " + marks + ", data: " + data);
    }
}
