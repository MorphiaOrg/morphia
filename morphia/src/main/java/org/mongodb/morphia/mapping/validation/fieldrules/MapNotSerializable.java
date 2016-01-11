package org.mongodb.morphia.mapping.validation.fieldrules;


import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;
import org.mongodb.morphia.utils.ReflectionUtils;

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
                    if (!Serializable.class.isAssignableFrom(keyClass)) {
                        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                                       "Value class (" + valueClass.getName() + ") is not Serializable"));
                    }
                }
            }
        }
    }
}
