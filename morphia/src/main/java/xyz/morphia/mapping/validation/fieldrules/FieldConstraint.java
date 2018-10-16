package xyz.morphia.mapping.validation.fieldrules;


import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.mapping.validation.ClassConstraint;
import xyz.morphia.mapping.validation.ConstraintViolation;

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
