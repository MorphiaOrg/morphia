package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceOnArray extends FieldConstraint {

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        final Reference ref = mf.getAnnotation(Reference.class);
        if (ref != null && ref.lazy()) {
            final Class type = mf.getType();
            if (type.isArray()) {
                ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                               "The lazy attribute cannot be used for an Array. If you need a lazy array "
                                               + "please use ArrayList instead."));
            }
        }
    }

}
