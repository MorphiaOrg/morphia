package org.mongodb.morphia.mapping.validation.classrules;


import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

import static java.lang.String.format;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EntityAndEmbed implements ClassConstraint {

    @Override
    public void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {

        if (mc.getEntityAnnotation() != null && mc.getEmbeddedAnnotation() != null) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(), format("Cannot have both @%s and @%s annotation at class level.",
                Entity.class.getSimpleName(), Embedded.class.getSimpleName())));
        }

    }
}
