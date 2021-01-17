package dev.morphia.mapping.validation.classrules;

import dev.morphia.annotations.Id;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.List;
import java.util.Set;

/**
 * Checks that only one field is marked with @Id
 */
public class MultipleId implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        final List<PropertyModel> properties = entityModel.getProperties(Id.class);

        if (properties.size() > 1) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(),
                Sofia.multipleIdPropertiesFound(new FieldEnumString(properties))));
        }
    }

}
