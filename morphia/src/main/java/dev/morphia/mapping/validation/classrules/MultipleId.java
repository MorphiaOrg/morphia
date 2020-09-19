package dev.morphia.mapping.validation.classrules;


import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.List;
import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MultipleId implements ClassConstraint {

    @Override
    public void check(Mapper mapper, MappedClass mc, Set<ConstraintViolation> ve) {

        final List<MappedField> idFields = mc.getFields(Id.class);

        if (idFields.size() > 1) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(),
                                           String.format("More than one @%s Field found (%s).",
                                                         Id.class.getSimpleName(),
                                                         new FieldEnumString(idFields))));
        }
    }

}
