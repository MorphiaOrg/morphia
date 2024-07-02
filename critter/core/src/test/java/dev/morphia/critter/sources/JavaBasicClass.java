package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;

import org.jetbrains.annotations.NotNull;

@Entity(value = "basicJava", useDiscriminator = false)
public class JavaBasicClass {
    public JavaBasicClass() {
    }

    public String nothing() {
        return "hi";
    }

    @NotNull
    public boolean useDiscriminator() {
        return false;
    }

    public Integer imAMethodYo() {
        return 0;
    }
}