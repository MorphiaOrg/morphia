package dev.morphia.mapping.validation.classrules;

import dev.morphia.annotations.Id;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.List;
import java.util.Set;

/**
 * Checks that only one field is marked with @Id
 */
public class MultipleId implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        final List<FieldModel> idFields = entityModel.getFields(Id.class);

        if (idFields.size() > 1) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(),
                String.format("More than one @Id field found (%s).", new FieldEnumString(idFields))));
        }
    }

}
