package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public abstract class FieldConstraint implements ClassConstraint {
    @Override
    public final void check(Mapper mapper, MappedClass mc, Set<ConstraintViolation> ve) {
        for (FieldModel mf : mc.getFields()) {
            check(mapper, mc, mf, ve);
        }
    }

    protected abstract void check(Mapper mapper, MappedClass mc, FieldModel mf, Set<ConstraintViolation> ve);

}
