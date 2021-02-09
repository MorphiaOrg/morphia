package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
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
public class MapKeyTypeConstraint extends PropertyConstraint {
    private static final String SUPPORTED = "(Map<String/Enum/Long/ObjectId/..., ?>)";

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve) {
        if (propertyModel.isMap()) {
            Class<?> aClass = null;
            List<TypeData<?>> typeParameters = propertyModel.getTypeData().getTypeParameters();
            if (!typeParameters.isEmpty()) {
                TypeData<?> typeData = typeParameters.get(0);
                aClass = typeData.getType();
            }
            // WARN if not parameterized : null or Object...
            if (aClass == null || Object.class.equals(aClass)) {
                ve.add(new ConstraintViolation(Level.WARNING, entityModel, propertyModel, getClass(),
                    "Maps cannot be keyed by Object (Map<Object,?>); Use a parametrized type that is supported "
                    + SUPPORTED));
            } else if (!aClass.equals(String.class) && !aClass.equals(ObjectId.class) && !isPrimitiveLike(aClass)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                    "Maps must be keyed by a simple type " + SUPPORTED + "; " + aClass
                    + " is not supported as a map key type."));
            }
        }
    }

    private boolean isPrimitiveLike(Class<?> type) {
        return List.of(
            Character.class, char.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Double.class, double.class,
            Float.class, float.class,
            Boolean.class, boolean.class,
            Byte.class, byte.class,
            String.class,
            Date.class,
            Locale.class,
            Class.class,
            UUID.class,
            URI.class)
                   .contains(type) || type.isEnum();

    }
}
