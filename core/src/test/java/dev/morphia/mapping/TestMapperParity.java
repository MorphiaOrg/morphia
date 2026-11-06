package dev.morphia.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.codec.pojo.ClassHierarchy;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.bson.codecs.pojo.PropertyAccessor;
import org.junit.jupiter.api.Test;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import static dev.morphia.mapping.PropertyDiscovery.FIELDS;
import static dev.morphia.mapping.PropertyDiscovery.METHODS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Maps every test entity with both mappers, under each property discovery mode, and compares the models they build.
 * Critter keeps its own discovery, from classfiles rather than reflection, so this is what keeps the two from drifting
 * apart.
 * <p>
 * A model is reduced to named facts: the entity's names and discriminator settings, its Morphia annotations, its id and
 * version properties, its lifecycle events, and for each property its names, types, Morphia annotations and what its
 * accessor reads and writes. Every fact that differs must be claimed by one of the {@link Known} divergences, and every
 * known divergence must still claim one, so a new divergence fails here and so does a fixed one that's still listed.
 */
public class TestMapperParity {

    private static final String MAPPING = "mapping";

    private static final String FALLBACK = "fallback";

    private static final List<Class<? extends Annotation>> MORPHIA_ANNOTATIONS = morphiaAnnotations();

    /**
     * The ways critter and reflection are known to model an entity differently. Each one names the plan item that closes
     * it; closing one means deleting it here, and the test then holds the mappers to the fix. A rule for a difference in
     * discovery claims every fact of the entities it applies to, since their properties differ wholesale; the others
     * claim only the facts they explain.
     */
    enum Known {
        /**
         * Critter can't generate a model with an array property, so it maps the entity reflectively.
         */
        ARRAYS("4.8") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return fact.equals(FALLBACK) && comparison.reflection.entrySet().stream()
                        .anyMatch(e -> e.getKey().endsWith(".array") && e.getValue().equals("true"));
            }
        },
        /**
         * Under field discovery, critter maps an entity through its getters instead when any getter carries a Morphia
         * annotation, and then maps only the annotated ones.
         */
        ANNOTATED_GETTERS("4.6") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == FIELDS && hasAnnotatedGetter(comparison.entity);
            }
        },
        /**
         * Under method discovery, critter maps an entity's fields when it has no getter and setter pairs; reflection maps
         * no properties at all, so it also fails on an entity whose id it then can't find.
         */
        FIELDS_WITHOUT_ACCESSORS("4.6") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == METHODS && !hasAccessorPair(comparison.entity);
            }
        },
        /**
         * Under method discovery, reflection pairs a superclass's private getter with its setter; critter skips the
         * getter, finds no pairs and maps the fields.
         */
        PRIVATE_INHERITED_GETTERS("4.6") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == METHODS && hasPrivateInheritedGetter(comparison.entity);
            }
        },
        /**
         * Under method discovery, critter doesn't take a setter that returns a value as a setter, so an entity whose
         * setters are all fluent has no pairs and critter maps its fields.
         */
        FLUENT_SETTERS("4.8") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == METHODS && hasFluentSetter(comparison.entity);
            }
        },
        /**
         * Under method discovery, critter keeps the annotations of a getter that overrides a superclass's; reflection
         * doesn't.
         */
        OVERRIDING_GETTER_ANNOTATIONS("4.6") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == METHODS && fact.endsWith(".annotations")
                        && overridesWithAnnotatedGetter(comparison.entity);
            }
        },
        /**
         * Under method discovery, reflection fails on a method named just {@code get} or {@code is} while working out a
         * property name from it; critter ignores such methods.
         */
        BARE_ACCESSOR_NAMES("reflection") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == METHODS && fact.equals(MAPPING)
                        && StringIndexOutOfBoundsException.class.getName().equals(comparison.reflection.get(MAPPING));
            }
        },
        /**
         * A shadowed field fails reflective mapping as a duplicate property, while critter maps the subclass's field.
         */
        SHADOWED_FIELDS("4.8") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == FIELDS && fact.equals(MAPPING) && shadowsField(comparison.entity);
            }
        },
        /**
         * Reflection takes a property's type from {@code @Property(concreteClass)}; critter keeps the declared type.
         */
        CONCRETE_CLASS("4.8") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                if (!fact.endsWith(".type") && !fact.endsWith(".typeData")) {
                    return false;
                }
                String annotations = comparison.reflection.get(property(fact) + ".annotations");
                return annotations != null && annotations.contains("@Property{")
                        && !annotations.contains("concreteClass=java.lang.Object");
            }
        },
        /**
         * Reflection's method accessors don't make their methods accessible, so they throw on any entity Morphia can't
         * otherwise reach, such as a package-private class; critter's accessors work.
         */
        INACCESSIBLE_METHODS("reflection") {
            @Override
            boolean claims(Comparison comparison, String fact) {
                return comparison.discovery == METHODS && fact.endsWith(".accessor")
                        && MappingException.class.getName().equals(comparison.reflection.get(fact));
            }
        };

        private final String closedBy;

        Known(String closedBy) {
            this.closedBy = closedBy;
        }

        abstract boolean claims(Comparison comparison, String fact);

        @Override
        public String toString() {
            return name() + " (" + closedBy + ")";
        }
    }

    /**
     * One entity's facts from each mapper under one discovery mode.
     */
    record Comparison(Class<?> entity, PropertyDiscovery discovery, Map<String, String> reflection, Map<String, String> critter) {
        String label() {
            return discovery == FIELDS ? entity.getName() : entity.getName() + " " + discovery;
        }

        /**
         * The facts to compare. When either mapper couldn't map the entity, or critter fell back to reflection, only that
         * outcome is compared: the rest would differ wholesale or compare reflection with itself.
         */
        Set<String> compared() {
            Set<String> outcomes = Set.of(MAPPING, FALLBACK);
            if (outcomes.stream().anyMatch(o -> reflection.containsKey(o) || critter.containsKey(o))) {
                return outcomes;
            }
            Set<String> keys = new TreeSet<>(reflection.keySet());
            keys.addAll(critter.keySet());
            return keys;
        }
    }

    @Test
    public void parity() {
        List<Class<?>> entities = testEntities();
        assertFalse(entities.isEmpty(), "found no test entities");

        Map<String, String> unknown = new TreeMap<>();
        Set<Known> claiming = new HashSet<>();
        boolean skipped = false;
        for (Class<?> entity : entities) {
            for (PropertyDiscovery discovery : PropertyDiscovery.values()) {
                Comparison comparison = compare(entity, discovery);
                if (comparison == null) {
                    skipped = true;
                    continue;
                }
                for (String fact : comparison.compared()) {
                    String reflection = comparison.reflection.get(fact);
                    String critter = comparison.critter.get(fact);
                    if (!Objects.equals(reflection, critter)) {
                        List<Known> known = List.of(Known.values()).stream()
                                .filter(k -> k.claims(comparison, fact))
                                .toList();
                        if (known.isEmpty()) {
                            unknown.put(comparison.label() + " " + fact, "reflection=" + reflection + ", critter=" + critter);
                        }
                        claiming.addAll(known);
                    }
                }
            }
        }

        assertEquals(Map.of(), unknown, () -> "critter and reflection model these differently:\n"
                + unknown.entrySet().stream()
                        .map(e -> "  " + e.getKey() + ": " + e.getValue())
                        .collect(Collectors.joining("\n")));
        // with pre-generated models some comparisons are skipped, so a rule can go unclaimed without being fixed; the
        // builds without them still catch a stale one
        if (!skipped) {
            Set<Known> stale = new TreeSet<>(Set.of(Known.values()));
            stale.removeAll(claiming);
            assertEquals(Set.of(), stale, "known divergences that no longer occur");
        }
    }

    /**
     * @return the comparison, or null when there is nothing to compare: a pre-generated model keeps the discovery mode
     *         it was built with whatever the runtime configuration says (plan item 4.12), so it's compared only under
     *         field discovery, the build's
     */
    private static Comparison compare(Class<?> entity, PropertyDiscovery discovery) {
        MorphiaConfig config = MorphiaConfig.load().propertyDiscovery(discovery);
        Supplier<Object> instances = () -> instantiate(entity);
        Object prototype = instances.get();
        Map<String, String> critter = new LinkedHashMap<>();
        EntityModel critterModel = facts(new CritterMapper(config.mapper(MapperType.CRITTER)), entity, prototype, instances,
                critter);
        if (discovery != FIELDS && critterModel instanceof CritterEntityModel
                && !(critterModel.getClass().getClassLoader() instanceof CritterClassLoader)) {
            return null;
        }
        Map<String, String> reflection = new LinkedHashMap<>();
        facts(new ReflectiveMapper(config.mapper(MapperType.REFLECTION)), entity, prototype, instances, reflection);
        return new Comparison(entity, discovery, reflection, critter);
    }

    /**
     * Every class under {@code dev.morphia} on the test classpath that the mappers would map as an entity: annotated
     * {@code @Entity} directly or through its hierarchy and not an {@code @ExternalEntity} stand-in, whose model is
     * named after its target.
     */
    private static List<Class<?>> testEntities() {
        List<Class<?>> entities = new ArrayList<>();
        try (ScanResult scan = new ClassGraph()
                .enableClasspath()
                .enableClassInfo()
                .enableAnnotationInfo()
                .ignoreClassVisibility()
                .acceptPackages("dev.morphia")
                // interfaces outside the scanned packages can carry @Entity
                .enableExternalClasses()
                .scan()) {
            for (ClassInfo info : scan.getAllStandardClasses()) {
                if (!info.isExternalClass() && mayBeEntity(info)) {
                    Class<?> type;
                    try {
                        type = Class.forName(info.getName(), false, TestMapperParity.class.getClassLoader());
                    } catch (ClassNotFoundException | LinkageError e) {
                        continue;
                    }
                    Map<Class<? extends Annotation>, Annotation> annotations = ClassHierarchy.annotations(type);
                    if (annotations.containsKey(Entity.class) && !annotations.containsKey(ExternalEntity.class)) {
                        entities.add(type);
                    }
                }
            }
        }
        entities.sort(Comparator.comparing(Class::getName));
        return entities;
    }

    /**
     * The same cheap check critter-maven makes before loading a class: {@code @Entity} is {@code @Inherited}, so
     * {@code hasAnnotation} covers superclasses, and interfaces are checked separately.
     */
    private static boolean mayBeEntity(ClassInfo info) {
        String entity = Entity.class.getName();
        return info.hasAnnotation(entity) || info.getAllSuperinterfaces().stream().anyMatch(i -> i.hasAnnotation(entity));
    }

    /**
     * Maps {@code entity} and records its facts.
     *
     * @return the model, or null if mapping failed
     */
    private static EntityModel facts(Mapper mapper, Class<?> entity, Object prototype, Supplier<Object> instances,
            Map<String, String> facts) {
        EntityModel model;
        try {
            model = mapper.mapEntity(entity);
        } catch (RuntimeException e) {
            facts.put(MAPPING, e.getClass().getName());
            return null;
        }
        if (mapper instanceof CritterMapper && !(model instanceof CritterEntityModel)) {
            facts.put(FALLBACK, "reflection");
            return model;
        }
        facts.put("name", model.getName());
        facts.put("collectionName", model.collectionName());
        facts.put("discriminator", model.discriminator());
        facts.put("discriminatorKey", model.discriminatorKey());
        facts.put("useDiscriminator", String.valueOf(model.useDiscriminator()));
        facts.put("abstract", String.valueOf(model.isAbstract()));
        facts.put("superClass", model.getSuperClass() == null ? null : model.getSuperClass().getType().getName());
        facts.put("id", name(model.getIdProperty()));
        facts.put("version", name(model.getVersionProperty()));
        facts.put("shardKeys", model.getShardKeys().stream().map(PropertyModel::getName).toList().toString());
        for (Class<? extends Annotation> lifecycle : Mapper.LIFECYCLE_ANNOTATIONS) {
            String event = "@" + lifecycle.getSimpleName();
            facts.put(event, String.valueOf(model.hasLifecycle(lifecycle)));
            facts.put(event + " document", String.valueOf(model.lifecycleUsesDocument(lifecycle)));
        }
        for (Class<? extends Annotation> type : MORPHIA_ANNOTATIONS) {
            Annotation annotation = model.getAnnotation(type);
            if (annotation != null) {
                facts.put("@" + type.getSimpleName(), render(annotation));
            }
        }
        facts.put("properties", model.getProperties().stream().map(PropertyModel::getName).sorted().toList().toString());
        for (PropertyModel property : model.getProperties()) {
            String prefix = property.getName() + ".";
            facts.put(prefix + "mappedName", property.getMappedName());
            facts.put(prefix + "loadNames", property.getLoadNames().toString());
            facts.put(prefix + "type", property.getType().getName());
            facts.put(prefix + "typeData", property.getTypeData().toString());
            facts.put(prefix + "normalizedType", property.getNormalizedType().getName());
            // TypeData's, since PropertyModel.isArray() misses a generic T[], which erases to Object
            facts.put(prefix + "array", String.valueOf(property.getTypeData().isArray()));
            facts.put(prefix + "final", String.valueOf(property.isFinal()));
            facts.put(prefix + "transient", String.valueOf(property.isTransient()));
            facts.put(prefix + "reference", String.valueOf(property.isReference()));
            facts.put(prefix + "annotations", property.getAnnotations().stream()
                    .filter(a -> MORPHIA_ANNOTATIONS.contains(a.annotationType()))
                    .map(TestMapperParity::render)
                    .sorted()
                    .toList()
                    .toString());
            if (prototype != null) {
                facts.put(prefix + "accessor", accessor(property, prototype, instances));
            }
        }
        return model;
    }

    private static String name(PropertyModel property) {
        return property == null ? null : property.getName();
    }

    private static String property(String fact) {
        return fact.substring(0, fact.lastIndexOf('.'));
    }

    /**
     * What a property's accessor reads from the shared prototype, and, for a type with a sample value, what it reads
     * back after writing the sample to a fresh instance. Both mappers read the same prototype, so values a constructor
     * generates, such as ids and timestamps, compare equal.
     */
    private static String accessor(PropertyModel property, Object prototype, Supplier<Object> instances) {
        PropertyAccessor<Object> accessor = property.getAccessor();
        Object sample = sample(property.getType());
        try {
            String initial = String.valueOf(accessor.get(prototype));
            if (sample == null) {
                return initial;
            }
            Object instance = instances.get();
            accessor.set(instance, sample);
            return initial + " -> " + accessor.get(instance);
        } catch (RuntimeException e) {
            return e.getClass().getName();
        }
    }

    private static Object sample(Class<?> type) {
        if (type == String.class) {
            return "parity";
        } else if (type == int.class || type == Integer.class) {
            return 42;
        } else if (type == long.class || type == Long.class) {
            return 42L;
        } else if (type == boolean.class || type == Boolean.class) {
            return true;
        } else if (type == double.class || type == Double.class) {
            return 4.2;
        }
        return null;
    }

    private static Object instantiate(Class<?> entity) {
        if (Modifier.isAbstract(entity.getModifiers())) {
            return null;
        }
        try {
            Constructor<?> constructor = entity.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static boolean hasAnnotatedGetter(Class<?> entity) {
        for (Class<?> type = entity; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class && hasMorphiaAnnotation(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAccessorPair(Class<?> entity) {
        Set<String> getters = new HashSet<>();
        Set<String> setters = new HashSet<>();
        for (Class<?> type = entity; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                String name = method.getName();
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                    if (name.startsWith("get") && name.length() > 3) {
                        getters.add(name.substring(3));
                    } else if (name.startsWith("is") && name.length() > 2) {
                        getters.add(name.substring(2));
                    }
                } else if (method.getParameterCount() == 1 && name.startsWith("set") && name.length() > 3) {
                    setters.add(name.substring(3));
                }
            }
        }
        getters.retainAll(setters);
        return !getters.isEmpty();
    }

    private static boolean hasPrivateInheritedGetter(Class<?> entity) {
        for (Class<?> type = entity.getSuperclass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isPrivate(method.getModifiers()) && method.getParameterCount() == 0
                        && method.getName().startsWith("get")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasFluentSetter(Class<?> entity) {
        for (Class<?> type = entity; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().startsWith("set") && method.getParameterCount() == 1
                        && method.getReturnType() != void.class) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overridesWithAnnotatedGetter(Class<?> entity) {
        for (Method method : entity.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && hasMorphiaAnnotation(method)) {
                for (Class<?> type = entity.getSuperclass(); type != null; type = type.getSuperclass()) {
                    try {
                        type.getDeclaredMethod(method.getName());
                        return true;
                    } catch (NoSuchMethodException e) {
                        // keep looking up the hierarchy
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasMorphiaAnnotation(Method method) {
        return List.of(method.getAnnotations()).stream().anyMatch(a -> MORPHIA_ANNOTATIONS.contains(a.annotationType()));
    }

    private static boolean shadowsField(Class<?> entity) {
        Set<String> names = new HashSet<>();
        for (Class<?> type = entity; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !names.add(field.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Renders an annotation from its members rather than its {@code toString()}, which differs between the JDK's
     * annotations and the builders' that critter models use.
     */
    private static String render(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        Map<String, String> members = new TreeMap<>();
        for (Method member : type.getDeclaredMethods()) {
            try {
                member.setAccessible(true);
                members.put(member.getName(), value(member.invoke(annotation)));
            } catch (ReflectiveOperationException | RuntimeException e) {
                members.put(member.getName(), e.getClass().getName());
            }
        }
        return "@" + type.getSimpleName() + members;
    }

    private static String value(Object value) {
        if (value instanceof Annotation annotation) {
            return render(annotation);
        } else if (value instanceof Class<?> type) {
            return type.getName();
        } else if (value != null && value.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(value(Array.get(value, i)));
            }
            return values.toString();
        }
        return String.valueOf(value);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Class<? extends Annotation>> morphiaAnnotations() {
        try (ScanResult scan = new ClassGraph()
                .enableClasspath()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackagesNonRecursive(Entity.class.getPackageName())
                .scan()) {
            return scan.getAllAnnotations().stream()
                    .map(info -> load(info.getName()).asSubclass(Annotation.class))
                    .sorted(Comparator.comparing(Class::getName))
                    .collect(Collectors.toList());
        }
    }
}
