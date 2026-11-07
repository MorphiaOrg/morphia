package dev.morphia.mapping.conventions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

/**
 * The rules both mappers discover properties by. Reflection applies them to {@code java.lang.reflect} members and critter
 * to classfiles, so they take plain inputs: names, modifiers and annotations.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class DiscoveryRules {
    /**
     * The modifier bit for synthetic members, which {@link Modifier} doesn't expose. Bridge methods carry it too.
     */
    private static final int SYNTHETIC = 0x1000;

    private static final Set<String> TRANSIENT_ANNOTATIONS = Set.of(
            Transient.class.getName(),
            java.beans.Transient.class.getName());

    private DiscoveryRules() {
    }

    /**
     * @param modifiers   the field's modifiers
     * @param annotations the names of the field's annotation types
     * @return true if the field isn't mapped: it's static, {@code transient}, or annotated as transient
     */
    public static boolean isIgnoredField(int modifiers, Collection<String> annotations) {
        return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
                || annotations.stream().anyMatch(TRANSIENT_ANNOTATIONS::contains);
    }

    /**
     * @param annotation the name of an annotation type
     * @return true if the annotation marks a property as transient
     */
    public static boolean isTransientAnnotation(String annotation) {
        return TRANSIENT_ANNOTATIONS.contains(annotation);
    }

    /**
     * @param config     the mapping configuration
     * @param annotation looks up one of the property's annotations by type, returning null if it doesn't have one
     * @param name       the property's Java name
     * @return the name the property is stored under: the id's, a name an annotation gives, or the naming strategy's
     */
    public static String mappedName(MorphiaConfig config, Function<Class<? extends Annotation>, ? extends Annotation> annotation,
            String name) {
        Property property = (Property) annotation.apply(Property.class);
        Reference reference = (Reference) annotation.apply(Reference.class);
        Version version = (Version) annotation.apply(Version.class);

        if (annotation.apply(Id.class) != null) {
            return "_id";
        } else if (property != null && !Mapper.IGNORED_FIELDNAME.equals(property.value())) {
            return property.value();
        } else if (reference != null && !Mapper.IGNORED_FIELDNAME.equals(reference.value())) {
            return reference.value();
        } else if (version != null && !Mapper.IGNORED_FIELDNAME.equals(version.value())) {
            return version.value();
        } else {
            return config.propertyNaming().apply(name);
        }
    }

    /**
     * @param methodName a method name
     * @return the name of the property a method by this name reads or writes, e.g. {@code name} for {@code getName},
     *         {@code setName} or {@code isName}; null if it isn't named like an accessor
     */
    @Nullable
    public static String propertyName(String methodName) {
        for (String prefix : List.of("get", "set", "is")) {
            if (methodName.startsWith(prefix) && methodName.length() > prefix.length()) {
                return methodCase(methodName.substring(prefix.length()));
            }
        }
        return null;
    }

    /**
     * Pairs the getters and setters one class declares. A property has a pair when exactly two of the class's instance
     * methods are named for it; of those, the one whose name sorts first is the getter.
     *
     * @param methods   the methods a class declares
     * @param name      a method's name
     * @param modifiers a method's modifiers
     * @param <M>       the method representation
     * @return the pairs, in the order their properties' methods are first declared
     */
    public static <M> List<AccessorPair<M>> accessorPairs(Collection<M> methods, Function<M, String> name,
            ToIntFunction<M> modifiers) {
        Map<String, List<M>> byProperty = new LinkedHashMap<>();
        for (M method : methods) {
            int flags = modifiers.applyAsInt(method);
            String property = propertyName(name.apply(method));
            if (property != null && !Modifier.isStatic(flags) && (flags & SYNTHETIC) == 0) {
                byProperty.computeIfAbsent(property, p -> new ArrayList<>()).add(method);
            }
        }
        List<AccessorPair<M>> pairs = new ArrayList<>();
        byProperty.forEach((property, candidates) -> {
            if (candidates.size() == 2) {
                List<M> sorted = candidates.stream().sorted(Comparator.comparing(name)).toList();
                pairs.add(new AccessorPair<>(property, sorted.get(0), sorted.get(1)));
            }
        });
        return pairs;
    }

    /**
     * @param s a name
     * @return the name with its first character upper-cased
     */
    public static String titleCase(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * @param s a name
     * @return the name with its first character lower-cased
     */
    public static String methodCase(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * A property's getter and setter.
     *
     * @param property the property's name
     * @param getter   the getter
     * @param setter   the setter
     * @param <M>      the method representation
     */
    public record AccessorPair<M>(String property, M getter, M setter) {
    }
}
