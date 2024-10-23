package dev.morphia.config;

import dev.morphia.annotations.Property;

public class MorphiaPropertyAnnotationProvider implements PropertyAnnotationProvider<Property> {
    @Override
    public Property convertToMorphia(Property annotation) {
        return annotation;
    }

    @Override
    public Class<Property> provides() {
        return Property.class;
    }
}
