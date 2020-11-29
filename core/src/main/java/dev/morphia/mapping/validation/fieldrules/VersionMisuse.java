package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

import static java.lang.String.format;

/**
 * A constraint to validate any versioning field on a type
 */
public class VersionMisuse extends FieldConstraint {

    private final MorphiaInstanceCreator creator;

    /**
     * Creates a version validator.
     *
     * @param creator the ObjectFactory to use
     */
    public VersionMisuse(MorphiaInstanceCreator creator) {
        this.creator = creator;
    }

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, FieldModel mf, Set<ConstraintViolation> ve) {
        if (mf.hasAnnotation(Version.class) && !entityModel.isAbstract()) {
            final Class<?> type = mf.getField().getType();
            if (!Long.class.equals(type) && !long.class.equals(type)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, mf, getClass(),
                    format("@%s can only be used on a Long/long field.", Version.class.getSimpleName())));
            }
        }
    }

}
