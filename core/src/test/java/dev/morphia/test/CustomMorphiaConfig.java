package dev.morphia.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class that provides a custom MorphiaConfig.
 * The test class must implement MorphiaConfigProvider to provide the custom configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomMorphiaConfig {
}