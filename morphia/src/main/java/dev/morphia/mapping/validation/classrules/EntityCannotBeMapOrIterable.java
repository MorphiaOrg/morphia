package dev.morphia.mapping.validation.classrules;


import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Map;
import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EntityCannotBeMapOrIterable implements ClassConstraint {

    @Override
    public void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {

        if (mc.getEntityAnnotation() != null && (Map.class.isAssignableFrom(mc.getClazz())
                                                 || Iterable.class.isAssignableFrom(mc.getClazz()))) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(), "Entities cannot implement Map/Iterable"));
        }

    }
}
