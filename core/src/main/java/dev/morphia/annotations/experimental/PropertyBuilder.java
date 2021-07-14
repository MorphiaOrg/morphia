package dev.morphia.annotations.experimental;

import dev.morphia.annotations.Property;
import dev.morphia.annotations.builders.AnnotationBuilder;

/**
 * @morphia.internal
 * @since 2.3
 */
public class PropertyBuilder extends AnnotationBuilder<Property> implements Property {
    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    @Override
    public Class<Property> annotationType() {
        return Property.class;
    }

    @Override
    public Class<?> concreteClass() {
        return get("concreteClass");
    }

    @Override
    public String value() {
        return get("value");
    }

    public PropertyBuilder concreteClass(Class<?> concreteClass) {
        put("concreteClass", concreteClass);
        return this;
    }

    public PropertyBuilder value(String value) {
        put("value", value);
        return this;
    }
}
