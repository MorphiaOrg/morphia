package dev.morphia.mapping.validation.classrules;

import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.List;
import java.util.Set;

/**
 * Checks the multiple fields aren't annotated with @Version
 */
public class MultipleVersions implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        final List<PropertyModel> versionFields = entityModel.getProperties(Version.class);
        if (versionFields.size() > 1) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(),
                "Multiple @" + Version.class + " annotations are not allowed. ("
                + new FieldEnumString(versionFields)));
        }
    }
}
