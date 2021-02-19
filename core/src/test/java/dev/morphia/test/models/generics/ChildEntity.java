package dev.morphia.test.models.generics;

import dev.morphia.annotations.Entity;

@Entity(value = "children")
public class ChildEntity extends FatherEntity<Child> {

}
