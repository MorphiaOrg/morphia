package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import org.bson.types.ObjectId;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A constraint to validate key types of Map fields
 */
public class MapKeyTypeConstraint extends FieldConstraint {
    private static final String SUPPORTED = "(Map<String/Enum/Long/ObjectId/..., ?>)";

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, FieldModel mf, Set<ConstraintViolation> ve) {
        if (mf.isMap()) {
            Class<?> aClass = null;
            List<TypeData<?>> typeParameters = mf.getTypeData().getTypeParameters();
            if (!typeParameters.isEmpty()) {
                TypeData<?> typeData = typeParameters.get(0);
                aClass = typeData.getType();
            }
            // WARN if not parameterized : null or Object...
            if (aClass == null || Object.class.equals(aClass)) {
                ve.add(new ConstraintViolation(Level.WARNING, entityModel, mf, getClass(),
                    "Maps cannot be keyed by Object (Map<Object,?>); Use a parametrized type that is supported "
                    + SUPPORTED));
            } else if (!aClass.equals(String.class) && !aClass.equals(ObjectId.class) && !isPrimitiveLike(aClass)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, mf, getClass(),
                    "Maps must be keyed by a simple type " + SUPPORTED + "; " + aClass
                    + " is not supported as a map key type."));
            }
        }
    }

    private boolean isPrimitiveLike(Class<?> type) {
        return type != null && (type == String.class || type == char.class
                                || type == Character.class || type == short.class || type == Short.class
                                || type == Integer.class || type == int.class || type == Long.class || type == long.class
                                || type == Double.class || type == double.class || type == float.class || type == Float.class
                                || type == Boolean.class || type == boolean.class || type == Byte.class || type == byte.class
                                || type == Date.class || type == Locale.class || type == Class.class || type == UUID.class
                                || type == URI.class || type.isEnum());

    }
}
