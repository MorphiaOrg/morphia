package org.mongodb.morphia.mapping.validation.fieldrules;


import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author ScottHenandez
 */
public class IdDoesNotMix extends FieldConstraint {

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        // an @Id field can not be a Value, Reference, or Embedded
        if (mf.hasAnnotation(Id.class)) {
            if (mf.hasAnnotation(Reference.class) || mf.hasAnnotation(Embedded.class) || mf.hasAnnotation(Property.class)) {
                ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                               mf.getFullName() + " is annotated as @" + Id.class.getSimpleName()
                                               + " and cannot be mixed with other annotations (like @Reference)"));
            }
        }
    }
}
