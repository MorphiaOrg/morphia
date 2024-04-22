package dev.morphia.mapping.validation.classrules;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

/**
 * Checks for duplicated attribute names
 */
public class DuplicatedAttributeNames implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {

        final Set<String> foundNames = new HashSet<>();
        for (PropertyModel model : entityModel.getProperties()) {
            Optional<PropertyModel> first = entityModel.getProperties().stream()
                    .filter(property -> property != model)
                    .filter(property -> model.getMappedName().equals(property.getName()))
                    .findFirst();
            first.ifPresent(propertyModel -> ve.add(new ConstraintViolation(Level.FATAL, entityModel, model, getClass(),
                    Sofia.propertyMappedToFieldName(entityModel.getName(), model.getName()))));

            for (String name : model.getLoadNames()) {
                if (!foundNames.add(name)) {
                    ve.add(new ConstraintViolation(Level.FATAL, entityModel, model, getClass(),
                            "Mapping to MongoDB field name '" + name
                                    + "' is duplicated; you cannot map different java fields to the same MongoDB field."));
                }
            }
        }
    }
}
