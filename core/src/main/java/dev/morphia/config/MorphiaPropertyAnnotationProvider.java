package dev.morphia.config;

import dev.morphia.annotations.Property;

/** @hidden */
public class MorphiaPropertyAnnotationProvider implements PropertyAnnotationProvider<Property> {
    @Override
    public Class<Property> provides() {
        return Property.class;
    }
}
