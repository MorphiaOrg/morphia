package dev.morphia.critter.sources;

public class DummyEntity {
    private String name = "DummyEntity";

    @Override
    public String toString() {
        return "DummyEntity{" +
               "name='" + name + '\'' +
               '}';
    }

//    public String __readName() {
//        return name;
//    }
//
//    public void __writeName(final String name) {
//        this.name = name;
//    }
}

