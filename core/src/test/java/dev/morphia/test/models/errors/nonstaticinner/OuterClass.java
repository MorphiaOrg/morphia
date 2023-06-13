package dev.morphia.test.models.errors.nonstaticinner;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

public class OuterClass {
    @Entity
    public class NonStaticInnerClass {
        @Id
        private final long id = 1;
    }
}
