package dev.morphia.test.models;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import static java.util.Arrays.asList;

@Entity
public class Student {
    @Id
    long id;

    List<Grade> grades;
    public String notes;

    public Student() {
    }

    public Student(long id, Grade... grades) {
        this.id = id;
        this.grades = asList(grades);
    }

    @Override
    public String toString() {
        return ("id: " + id + ", grades: " + grades);
    }
}
