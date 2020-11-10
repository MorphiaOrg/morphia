package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;

import static java.util.Arrays.asList;

@Entity
public class Student {
    @Id
    private long id;

    private List<Grade> grades;

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
