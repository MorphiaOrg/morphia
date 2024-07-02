package dev.morphia.critter.sources;

public class DummyEntity {
    private String name = "DummyEntity";

    private int age = 21;
    private Long salary = 2L;

    @Override
    public String toString() {
        return "DummyEntity{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", salary=" + salary +
                '}';
    }

    public String __readNameSample() {
        return name;
    }

    public void __writeNameSample(final String name) {
        this.name = name;
    }

    public int __readAgeSample() {
        return age;
    }

    public void __writeAgeSample(final int age) {
        this.age = age;
    }
}
