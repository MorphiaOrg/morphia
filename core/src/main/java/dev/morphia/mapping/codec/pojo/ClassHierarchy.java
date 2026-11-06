package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * The superclasses and type-level annotations of a class, worked out once per class and shared by both mappers.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class ClassHierarchy {
    private static final ClassValue<List<Class<?>>> SUPERCLASSES = new ClassValue<>() {
        @Override
        protected List<Class<?>> computeValue(Class<?> type) {
            List<Class<?>> superclasses = new ArrayList<>();
            Class<?> superclass = type.getSuperclass();
            while (superclass != null && !superclass.isEnum() && !superclass.equals(Object.class)) {
                superclasses.add(superclass);
                superclass = superclass.getSuperclass();
            }
            return List.copyOf(superclasses);
        }
    };

    private static final ClassValue<Map<Class<? extends Annotation>, Annotation>> ANNOTATIONS = new ClassValue<>() {
        @Override
        protected Map<Class<? extends Annotation>, Annotation> computeValue(Class<?> type) {
            Map<Class<? extends Annotation>, Annotation> annotations = new LinkedHashMap<>();
            Set<Class<?>> visited = new HashSet<>();
            add(annotations, type);
            addInterfaces(annotations, type, visited);
            List<Class<?>> superclasses = superclasses(type);
            superclasses.forEach(superclass -> add(annotations, superclass));
            superclasses.forEach(superclass -> addInterfaces(annotations, superclass, visited));
            return Collections.unmodifiableMap(annotations);
        }
    };

    private ClassHierarchy() {
    }

    /**
     * @param type the type
     * @return the type's superclasses, nearest first, stopping before {@code Object} or an enum
     */
    public static List<Class<?>> superclasses(Class<?> type) {
        return SUPERCLASSES.get(type);
    }

    /**
     * Collects the annotations on a type, its superclasses and the interfaces any of them implement. When more than one
     * declares an annotation of the same type, the first one found wins, in this order:
     * <ol>
     * <li>the type itself, including the {@code @Inherited} annotations of its superclasses</li>
     * <li>the interfaces it implements, depth first in declaration order</li>
     * <li>its superclasses, nearest first</li>
     * <li>the interfaces its superclasses implement, depth first, nearest superclass first</li>
     * </ol>
     * Morphia's type-level annotations are all {@code @Inherited}, so for them a superclass comes before any interface.
     *
     * @param type the type
     * @return the annotations by annotation type, in the order found
     */
    public static Map<Class<? extends Annotation>, Annotation> annotations(Class<?> type) {
        return ANNOTATIONS.get(type);
    }

    private static void add(Map<Class<? extends Annotation>, Annotation> annotations, Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            annotations.putIfAbsent(annotation.annotationType(), annotation);
        }
    }

    private static void addInterfaces(Map<Class<? extends Annotation>, Annotation> annotations, Class<?> type,
            Set<Class<?>> visited) {
        for (Class<?> anInterface : type.getInterfaces()) {
            if (visited.add(anInterface)) {
                add(annotations, anInterface);
                addInterfaces(annotations, anInterface, visited);
            }
        }
    }
}
