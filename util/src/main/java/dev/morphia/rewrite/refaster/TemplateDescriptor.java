package dev.morphia.rewrite.refaster;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface TemplateDescriptor {
    String name();

    String description();
}
