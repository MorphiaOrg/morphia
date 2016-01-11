package org.mongodb.morphia.mapping.validation.classrules;


import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;


/**
 * @author josephpachod
 */
public class ContainsEmbeddedWithId implements ClassConstraint {

    @Override
    public void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {
        final Set<Class<?>> classesToInspect = new HashSet<Class<?>>();
        for (final Field field : ReflectionUtils.getDeclaredAndInheritedFields(mc.getClazz(), true)) {
            if (isFieldToInspect(field) && !field.isAnnotationPresent(Id.class)) {
                classesToInspect.add(field.getType());
            }
        }
        checkRecursivelyHasNoIdAnnotationPresent(classesToInspect, new HashSet<Class<?>>(), mc, ve);
    }

    private void checkRecursivelyHasNoIdAnnotationPresent(final Set<Class<?>> classesToInspect,
                                                          final HashSet<Class<?>> alreadyInspectedClasses, final MappedClass mc,
                                                          final Set<ConstraintViolation> ve) {
        for (final Class<?> clazz : classesToInspect) {
            if (alreadyInspectedClasses.contains(clazz)) {
                continue;
            }
            if (hasTypeFieldAnnotation(clazz, Id.class)) {
                ve.add(new ConstraintViolation(Level.FATAL,
                                               mc,
                                               getClass(),
                                               "You cannot use @Id on any field of an Embedded/Property object"));
            }
            alreadyInspectedClasses.add(clazz);
            final Set<Class<?>> extraClassesToInspect = new HashSet<Class<?>>();
            for (final Field field : ReflectionUtils.getDeclaredAndInheritedFields(clazz, true)) {
                if (isFieldToInspect(field)) {
                    extraClassesToInspect.add(field.getType());
                }
            }
            checkRecursivelyHasNoIdAnnotationPresent(extraClassesToInspect, alreadyInspectedClasses, mc, ve);
        }
    }

    private boolean hasTypeFieldAnnotation(final Class<?> type, final Class<Id> class1) {
        for (final Field field : ReflectionUtils.getDeclaredAndInheritedFields(type, true)) {
            if (field.getAnnotation(class1) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isFieldToInspect(final Field field) {
        return (!field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(Reference.class));
    }
}
