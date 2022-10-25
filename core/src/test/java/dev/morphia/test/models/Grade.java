package dev.morphia.test.models;

import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

@Entity
public class Grade {
    public int marks;

    @Property("d")
    public Map<String, String> data;

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
