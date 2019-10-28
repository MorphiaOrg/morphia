package dev.morphia.mapping.validation.fieldrules;


import org.bson.codecs.pojo.TypeData;
import org.bson.types.ObjectId;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


public class MapKeyTypeConstraint extends FieldConstraint {
    private static final String SUPPORTED = "(Map<String/Enum/Long/ObjectId/..., ?>)";

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        if (mf.isMap()) {
            Class aClass = null;
            List typeParameters = mf.getFieldModel().getTypeData().getTypeParameters();
            if(!typeParameters.isEmpty()) {
                TypeData typeData = (TypeData) typeParameters.get(0);
                aClass = typeData.getType();
            }
            // WARN if not parameterized : null or Object...
            if (aClass == null || Object.class.equals(aClass)) {
                ve.add(new ConstraintViolation(Level.WARNING, mc, mf, getClass(),
                                               "Maps cannot be keyed by Object (Map<Object,?>); Use a parametrized type that is supported "
                                               + SUPPORTED));
            } else if (!aClass.equals(String.class) && !aClass.equals(ObjectId.class) && !isPrimitiveLike(aClass)) {
                ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                               "Maps must be keyed by a simple type " + SUPPORTED + "; " + aClass
                                               + " is not supported as a map key type."));
            }
        }
    }

    private boolean isPrimitiveLike(final Class type) {
        return type != null && (type == String.class || type == char.class
                                || type == Character.class || type == short.class || type == Short.class
                                || type == Integer.class || type == int.class || type == Long.class || type == long.class
                                || type == Double.class || type == double.class || type == float.class || type == Float.class
                                || type == Boolean.class || type == boolean.class || type == Byte.class || type == byte.class
                                || type == Date.class || type == Locale.class || type == Class.class || type == UUID.class
                                || type == URI.class || type.isEnum());

    }
}
