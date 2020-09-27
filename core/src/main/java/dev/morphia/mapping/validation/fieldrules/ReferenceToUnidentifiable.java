package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.Key;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReferenceToUnidentifiable extends FieldConstraint {

    @Override
    protected void check(Mapper mapper, MappedClass mc, MappedField mf, Set<ConstraintViolation> ve) {
        if (mf.hasAnnotation(Reference.class)) {
            final Class realType = /*(mf.isScalarValue()) ? mf.getType() : */mf.getNormalizedType();

            if (realType == null) {
                throw new MappingException("Type is null for this MappedField: " + mf);
            }

            if (realType.equals(Key.class)) {
                ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(), Sofia.keyNotAllowedAsField()));
            } else {
                MappedClass mappedClass = mapper.getMappedClass(realType);
                if (mappedClass == null || mappedClass.getIdField() == null && !mappedClass.getType().isInterface()) {
                    ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                        mf.getFullName() + " is annotated as a @" + Reference.class.getSimpleName() + " but the "
                        + mf.getType().getName()
                        + " class is missing the @" + Id.class.getSimpleName() + " annotation"));
                }
            }
        }
    }

}
