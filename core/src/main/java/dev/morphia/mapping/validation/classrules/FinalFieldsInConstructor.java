package dev.morphia.mapping.validation.classrules;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.internal.ConstructorCreator;
import dev.morphia.mapping.internal.FinalFieldMutation;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

/**
 * Checks that a type's final properties are all passed to the constructor that creates its instances, so that none of them
 * has to be set reflectively (JEP 500). That's an error on a JVM that denies it and a warning anywhere else.
 */
public class FinalFieldsInConstructor implements ClassConstraint {
    private final BooleanSupplier denied;

    /**
     * Creates the rule for this JVM.
     */
    public FinalFieldsInConstructor() {
        this(FinalFieldMutation::denied);
    }

    /**
     * @param denied whether this JVM denies setting final fields reflectively
     */
    public FinalFieldsInConstructor(BooleanSupplier denied) {
        this.denied = denied;
    }

    @Override
    public void check(Mapper mapper, EntityModel model, Set<ConstraintViolation> ve) {
        // finals aren't stored when they're ignored, so there's nothing to set when loading
        if (Modifier.isAbstract(model.getType().getModifiers()) || mapper.getConfig().ignoreFinals()) {
            return;
        }
        List<String> reflective = ConstructorCreator.reflectiveFinals(model);
        if (!reflective.isEmpty()) {
            boolean deny = denied.getAsBoolean();
            String consequence = deny ? Sofia.finalFieldsDenied() : Sofia.finalFieldsRestricted();
            String type = model.getType().getName();
            String message = ConstructorCreator.hasLifecycleEvents(model)
                    ? Sofia.finalFieldsReflectiveLifecycle(type, reflective, consequence)
                    : Sofia.finalFieldsReflective(type, reflective, consequence);
            ve.add(new ConstraintViolation(deny ? Level.FATAL : Level.WARNING, model, getClass(), message));
        }
    }
}
