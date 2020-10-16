package dev.morphia.mapping.validation.classrules;

import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * Checks for duplicated attribute names
 */
public class DuplicatedAttributeNames implements ClassConstraint {

    @Override
    public void check(Mapper mapper, MappedClass mc, Set<ConstraintViolation> ve) {
        final Set<String> foundNames = new HashSet<>();
        for (FieldModel model : mc.getFields()) {
            for (String name : model.getLoadNames()) {
                if (!foundNames.add(name)) {
                    ve.add(new ConstraintViolation(Level.FATAL, mc, model, getClass(),
                                                   "Mapping to MongoDB field name '" + name
                                                   + "' is duplicated; you cannot map different java fields to the same MongoDB field."));
                }
            }
        }
    }
}
