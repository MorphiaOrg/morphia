package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
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
    protected void check(Mapper mapper, MappedClass mc, FieldModel mf, Set<ConstraintViolation> ve) {
        if (mf.hasAnnotation(Version.class) && !mc.isAbstract()) {
            final Class<?> type = mf.getField().getType();
            if (Long.class.equals(type) || long.class.equals(type)) {

                final Object testInstance = creator.getInstance();

                // check initial value
                if (Long.class.equals(type)) {
                    if (mf.getValue(testInstance) != null) {
                        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                            format("When using @%s on a Long field, it must be initialized to null.",
                                Version.class.getSimpleName())));
                    }
                } else if (long.class.equals(type)) {
                    if ((Long) mf.getValue(testInstance) != 0L) {
                        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                            format("When using @%s on a long field, it must be initialized to 0.",
                                Version.class.getSimpleName())));
                    }
                }
            } else {
                ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                    format("@%s can only be used on a Long/long field.", Version.class.getSimpleName())));
            }
        }
    }

}
