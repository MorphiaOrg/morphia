package org.mongodb.morphia.mapping.validation.fieldrules;


import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceMissingDependencies extends FieldConstraint {

    @Override
    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
        final Reference ref = mf.getAnnotation(Reference.class);
        if (ref != null) {
            if (ref.lazy()) {
                if (!LazyFeatureDependencies.testDependencyFullFilled()) {
                    ve.add(new ConstraintViolation(Level.SEVERE, mc, mf, getClass(),
                                                   "Lazy references need CGLib and Proxytoys in the classpath."));
                }
            }
        }
    }

}
