package xyz.morphia.mapping.validation.fieldrules;


import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Reference;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.mapping.MappingException;
import xyz.morphia.mapping.validation.ConstraintViolation;
import xyz.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReferenceToUnidentifiable extends FieldConstraint {

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        if (mf.hasAnnotation(Reference.class)) {
            final Class realType = (mf.isSingleValue()) ? mf.getType() : mf.getSubClass();

            if (realType == null) {
                throw new MappingException("Type is null for this MappedField: " + mf);
            }

            if ((!realType.isInterface() && mapper.getMappedClass(realType).getIdField() == null)) {
                ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
                                               mf.getFullName() + " is annotated as a @" + Reference.class.getSimpleName() + " but the "
                                               + mf.getType().getName()
                                               + " class is missing the @" + Id.class.getSimpleName() + " annotation"));
            }
        }
    }

}
