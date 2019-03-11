package dev.morphia.mapping.validation.classrules;


import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

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
