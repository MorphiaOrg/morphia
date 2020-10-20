package dev.morphia.mapping.validation.classrules;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

import static java.lang.String.format;

/**
 * Checks that @Entity and @Embed aren't both used on a type.
 */
public class EntityAndEmbed implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        if (entityModel.getEntityAnnotation() != null && entityModel.getEmbeddedAnnotation() != null) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(),
                format("Cannot have both @%s and @%s annotation at class level.",
                    Entity.class.getSimpleName(), Embedded.class.getSimpleName())));
        }

    }
}
