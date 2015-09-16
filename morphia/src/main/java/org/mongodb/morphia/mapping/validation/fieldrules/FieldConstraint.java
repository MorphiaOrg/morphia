package org.mongodb.morphia.mapping.validation.fieldrules;


import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public abstract class FieldConstraint implements ClassConstraint {
    @Override
    public final void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {
        for (final MappedField mf : mc.getPersistenceFields()) {
            check(mapper, mc, mf, ve);
        }
    }

    protected abstract void check(final Mapper mapper, MappedClass mc, MappedField mf, Set<ConstraintViolation> ve);

}
