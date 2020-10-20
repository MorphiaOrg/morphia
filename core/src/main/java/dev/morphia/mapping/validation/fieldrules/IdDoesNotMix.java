package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

/**
 * Checks that @Id is not with any other mapping annotation
 */
public class IdDoesNotMix extends FieldConstraint {

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, FieldModel mf, Set<ConstraintViolation> ve) {
        // an @Id field can not be a Value, Reference, or Embedded
        if (mf.hasAnnotation(Id.class)) {
            if (mf.hasAnnotation(Reference.class) || mf.hasAnnotation(Embedded.class) || mf.hasAnnotation(Property.class)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, mf, getClass(),
                    mf.getFullName() + " is annotated as @" + Id.class.getSimpleName()
                    + " and cannot be mixed with other annotations (like @Reference)"));
            }
        }
    }
}
