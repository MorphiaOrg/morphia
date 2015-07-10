package org.mongodb.morphia.mapping.validation.fieldrules;


import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MisplacedProperty extends FieldConstraint {

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        // a field can be a Value, Reference, or Embedded
        if (mf.hasAnnotation(Property.class)) {
            // make sure that the property type is supported
            if (mf.isSingleValue() && !mf.isTypeMongoCompatible() && !mapper.getConverters().hasSimpleValueConverter(mf)) {
                ve.add(new ConstraintViolation(Level.WARNING, mc, mf, getClass(),
                                               mf.getFullName() + " is annotated as @" + Property.class.getSimpleName()
                                               + " but is a type that cannot be mapped simply (type is "
                                               + mf.getType().getName() + ")."));
            }
        }
    }

}
