package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

import static java.lang.String.format;

/**
 * A constraint to validate any versioning field on a type
 */
public class VersionMisuse extends PropertyConstraint {
    @Override
    protected void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve) {
        if (propertyModel.hasAnnotation(Version.class) && !entityModel.isAbstract()) {
            final Class<?> type = propertyModel.getType();
            if (!Long.class.equals(type) && !long.class.equals(type)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                    format("@%s can only be used on a Long/long field.", Version.class.getSimpleName())));
            }
        }
    }
}
