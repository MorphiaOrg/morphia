package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.lazy.LazyFeatureDependencies;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceMissingDependencies extends FieldConstraint {

    @Override
    protected void check(Mapper mapper, MappedClass mc, FieldModel mf, Set<ConstraintViolation> ve) {
        final Reference ref = mf.getAnnotation(Reference.class);
        if (ref != null) {
            if (ref.lazy()) {
                if (!LazyFeatureDependencies.assertProxyClassesPresent()) {
                    ve.add(new ConstraintViolation(Level.SEVERE, mc, mf, getClass(),
                                                   "Lazy references need ByteBuddy the classpath."));
                }
            }
        }
    }

}
