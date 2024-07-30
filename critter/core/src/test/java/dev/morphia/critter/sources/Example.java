package dev.morphia.critter.sources;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

@Entity
public class Example {
    @Property("myName")
    @AlsoLoad({ "name1", "name2" })
    private String name;

    private int age = 21;
    @Property
    private Long salary = 2L;

    public String __readName() {
        return name;
    }

    public void __writeName(final String name) {
        this.name = name;
    }

    public int __readAge() {
        return age;
    }

    public void __writeAge(final int age) {
        this.age = age;
    }

    public Long __readSalary() {
        return salary;
    }

    public void __writeSalary(final Long salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "Example{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", salary=" + salary +
                '}';
    }
}
