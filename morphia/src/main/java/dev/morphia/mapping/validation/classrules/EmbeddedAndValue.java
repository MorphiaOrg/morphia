package dev.morphia.mapping.validation.classrules;


import dev.morphia.annotations.Embedded;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EmbeddedAndValue implements ClassConstraint {

    @Override
    public void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {

        if (mc.getEmbeddedAnnotation() != null && !mc.getEmbeddedAnnotation().value().equals(Mapper.IGNORED_FIELDNAME)) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(),
                                           "@" + Embedded.class.getSimpleName()
                                           + " classes cannot specify a fieldName value(); this is on applicable on fields"));
        }
    }

}
