package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public abstract class FieldConstraint implements ClassConstraint {
    @Override
    public final void check(Mapper mapper, MappedClass mc, Set<ConstraintViolation> ve) {
        for (MappedField mf : mc.getFields()) {
            check(mapper, mc, mf, ve);
        }
    }

    protected abstract void check(Mapper mapper, MappedClass mc, MappedField mf, Set<ConstraintViolation> ve);

}
