package dev.morphia.critter.sources;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;

@Entity
public class Example {
    @Property(value = "myName")
    @AlsoLoad({ "name1", "name2" })
    private String name;

    @Reference(idOnly = true)
    private int age = 21;
    @Property
    private Long salary = 2L;

    public String __readNameTemplate() {
        return name;
    }

    public void __writeNameTemplate(final String name) {
        this.name = name;
    }

    public int __readAgeTemplate() {
        return age;
    }

    public void __writeAgeTemplate(final int age) {
        this.age = age;
    }

    public Long __readSalaryTemplate() {
        return salary;
    }

    public void __writeSalaryTemplate(final Long salary) {
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
