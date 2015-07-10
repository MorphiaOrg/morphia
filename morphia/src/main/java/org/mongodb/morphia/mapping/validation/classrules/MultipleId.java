package org.mongodb.morphia.mapping.validation.classrules;


import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.List;
import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MultipleId implements ClassConstraint {

    @Override
    public void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {

        final List<MappedField> idFields = mc.getFieldsAnnotatedWith(Id.class);

        if (idFields.size() > 1) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(),
                                           String.format("More than one @%s Field found (%s).",
                                                         Id.class.getSimpleName(),
                                                         new FieldEnumString(idFields))));
        }
    }

}
