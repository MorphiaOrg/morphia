package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Serialized;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.utils.ReflectionUtils;

import java.io.Serializable;
import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapNotSerializable extends FieldConstraint {

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        if (mf.isMap()) {
            if (mf.hasAnnotation(Serialized.class)) {
                final Class<?> keyClass = ReflectionUtils.getParameterizedClass(mf.getField(), 0);
                final Class<?> valueClass = ReflectionUtils.getParameterizedClass(mf.getField(), 1);
                if (keyClass != null) {
                    if (!Serializable.class.isAssignableFrom(keyClass)) {
                        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                                       "Key class (" + keyClass.getName() + ") is not Serializable"));
                    }
                }
                if (valueClass != null) {
                    if (!Serializable.class.isAssignableFrom(valueClass)) {
                        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                                       "Value class (" + valueClass.getName() + ") is not Serializable"));
                    }
                }
            }
        }
    }
}
