package dev.morphia.mapping.validation.fieldrules;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Checks that contradicting annotations aren't defined.
 */
public class ContradictingAnnotations extends PropertyConstraint {

    private final Class<? extends Annotation> a1;
    private final Class<? extends Annotation> a2;

    /**
     * Creates a ContradictingFieldAnnotation validation with the two incompatible annotations.
     *
     * @param a1 the first annotation
     * @param a2 the second annotation
     */
    public ContradictingAnnotations(Class<? extends Annotation> a1, Class<? extends Annotation> a2) {
        this.a1 = a1;
        this.a2 = a2;
    }

    @Override
    protected final void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve) {
        if (propertyModel.hasAnnotation(a1) && propertyModel.hasAnnotation(a2)) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                Sofia.contradictingAnnotations(a1.getSimpleName(), a2.getSimpleName())));
        }
    }
}
