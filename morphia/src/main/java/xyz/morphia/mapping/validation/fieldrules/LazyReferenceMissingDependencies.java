package xyz.morphia.mapping.validation.fieldrules;


import xyz.morphia.annotations.Reference;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.mapping.lazy.LazyFeatureDependencies;
import xyz.morphia.mapping.validation.ConstraintViolation;
import xyz.morphia.mapping.validation.ConstraintViolation.Level;

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
