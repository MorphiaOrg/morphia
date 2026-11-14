package dev.morphia.mapping.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.Name;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.sofia.Sofia;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static java.lang.Integer.compare;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

/**
 * Defines a Creator that uses a full constructor to create an instance rather than field injection. This requires that a class have a
 * constructor that accepts a parameter for each mapped field on the class.
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class ConstructorCreator implements MorphiaInstanceCreator {
    private static final Pattern UNNAMED_PARAMETER = Pattern.compile("arg[0-9]+");
    private static final Object[] NO_PARAMETERS = new Object[0];
    // the boxed numeric types in the order primitive widening follows: each widens to those after it
    private static final List<Class<?>> WIDENING = List.of(Byte.class, Short.class, Integer.class, Long.class, Float.class,
            Double.class);

    private final Plan plan;
    private final Conversions conversions;
    private final Object[] parameters;
    // properties the constructor doesn't take, set once it has run; allocated only if a value arrives before then
    @Nullable
    private List<PropertyModel> pendingModels;
    @Nullable
    private List<Object> pendingValues;
    private Object instance;

    /**
     * @param model       the model
     * @param constructor the constructor to use
     * @param conversions the Conversions instance to use
     */
    public ConstructorCreator(EntityModel model, Constructor<?> constructor, Conversions conversions) {
        this(new Plan(model, constructor), conversions);
    }

    /**
     * @param plan        the precomputed constructor details
     * @param conversions the Conversions instance to use
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ConstructorCreator(Plan plan, Conversions conversions) {
        this.plan = plan;
        this.conversions = conversions;
        this.parameters = plan.types.length == 0 ? NO_PARAMETERS : plan.zeroValues.clone();
    }

    /**
     * The per-constructor work, done once per model rather than once per decoded instance.
     */
    public static final class Plan {
        private final EntityModel model;
        private final Instantiator instantiator;
        private final Map<String, Integer> positions = new LinkedHashMap<>();
        private final Class<?>[] types;
        // types with primitives boxed: an argument that's already an instance of its slot's type needs no conversion
        private final Class<?>[] boxedTypes;
        private final Object[] zeroValues;

        /**
         * @param model       the model
         * @param constructor the constructor to use
         */
        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Plan(EntityModel model, Constructor<?> constructor) {
            this.model = model;
            Instantiator generated = model.getInstantiator(constructor);
            this.instantiator = generated != null ? generated : reflective(constructor);
            final Parameter[] constructorParameters = constructor.getParameters();
            types = new Class<?>[constructorParameters.length];
            boxedTypes = new Class<?>[constructorParameters.length];
            zeroValues = new Object[constructorParameters.length];
            for (int i = 0; i < constructorParameters.length; i++) {
                final Parameter parameter = constructorParameters[i];
                types[i] = parameter.getType();
                boxedTypes[i] = Conversions.boxedType(types[i]);
                zeroValues[i] = zeroValue(types[i]);
                String name = getParameterName(parameter);
                if (UNNAMED_PARAMETER.matcher(name).matches()) {
                    throw new MappingException(Sofia.unnamedConstructorParameter(model.getType().getName()));
                }
                if (positions.put(name, i) != null) {
                    throw new MappingException(Sofia.duplicatedParameterName(model.getType().getName(), name));
                }
            }
        }
    }

    /**
     * Calls the constructor reflectively, throwing whatever it throws as a generated instantiator would.
     */
    private static Instantiator reflective(Constructor<?> constructor) {
        constructor.setAccessible(true);
        return arguments -> {
            try {
                return constructor.newInstance(arguments);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception exception) {
                    throw exception;
                } else if (e.getCause() instanceof Error error) {
                    throw error;
                }
                throw e;
            }
        };
    }

    @Nullable
    public static Constructor<?> bestConstructor(EntityModel model) {
        var propertyMap = new TreeMap<String, Class<?>>();
        model.getProperties()
                .forEach(it -> propertyMap.put(it.getName(), it.getType()));

        var constructors = asList(model.getType().getDeclaredConstructors());

        if (hasLifecycleEvents(model)) {
            return constructors.stream()
                    .filter(it -> it.getParameters().length == 0)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(Sofia.lifecycleNoargs(model.getType())));
        }

        // a constructor taking every final property comes first so that none of them has to be set reflectively (JEP 500),
        // then the one taking the most properties
        List<String> finals = finalProperties(model);
        return constructors.stream()
                .filter(it -> stream(it.getParameters())
                        .allMatch(param -> Objects.equals(propertyMap.get(getParameterName(param)), param.getType())))
                .sorted(Comparator.<Constructor<?>> comparingInt(it -> missing(finals, it).size())
                        .thenComparing((o1, o2) -> compare(o2.getParameterCount(), o1.getParameterCount())))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param model the model
     * @return the names of the final properties the constructor used to create {@code model}'s instances doesn't take, which
     *         must then be set reflectively
     * @morphia.internal
     */
    @MorphiaInternal
    public static List<String> reflectiveFinals(EntityModel model) {
        List<String> finals = finalProperties(model);
        if (finals.isEmpty()) {
            return finals;
        }
        if (hasLifecycleEvents(model)) {
            // created with the no-arg constructor
            return finals;
        }
        Constructor<?> constructor = bestConstructor(model);
        if (constructor == null) {
            // instances are created with the full constructor. its parameters may name properties by their mapped names,
            // which don't bind, so those finals are still set reflectively
            try {
                constructor = getFullConstructor(model);
            } catch (MappingException e) {
                // no instances can be created at all, which mapping reports on its own
                return List.of();
            }
        }
        return missing(finals, constructor);
    }

    private static List<String> finalProperties(EntityModel model) {
        // a final ignored by the configuration isn't stored, so there's nothing to pass a constructor
        return model.getProperties().stream()
                .filter(property -> property.isFinal() && !property.isIgnoredFinal())
                .map(PropertyModel::getName)
                .toList();
    }

    private static List<String> missing(List<String> finals, Constructor<?> constructor) {
        List<String> missing = new ArrayList<>(finals);
        for (Parameter parameter : constructor.getParameters()) {
            missing.remove(getParameterName(parameter));
        }
        return missing;
    }

    /**
     * @param model the model
     * @return true if the type has lifecycle methods, which requires a no-arg constructor
     * @morphia.internal
     */
    @MorphiaInternal
    public static boolean hasLifecycleEvents(EntityModel model) {
        return model.hasLifecycle(PreLoad.class)
                || model.hasLifecycle(PostLoad.class)
                || model.hasLifecycle(PrePersist.class)
                || model.hasLifecycle(PostPersist.class);
    }

    @Override
    public Object getInstance() {
        if (instance == null) {
            try {
                instance = plan.instantiator.newInstance(parameters);
                if (pendingModels != null) {
                    for (int i = 0; i < pendingModels.size(); i++) {
                        pendingModels.get(i).setValue(instance, pendingValues.get(i));
                    }
                }
            } catch (Exception e) {
                throw new MappingException(Sofia.cannotInstantiate(plan.model.getType().getName(), e.getMessage()), e);
            }
        }
        return instance;
    }

    @Override
    public boolean isEagerInstanceSafe() {
        return plan.positions.isEmpty();
    }

    /**
     * @param model the model to check
     * @return the constructor taking all fields if it exists
     * @morphia.internal
     */
    @MorphiaInternal
    public static Constructor<?> getFullConstructor(EntityModel model) {
        for (Constructor<?> constructor : model.getType().getDeclaredConstructors()) {
            if (constructor.getParameterCount() == model.getProperties().size() && namesMatchProperties(model, constructor)) {
                return constructor;
            }
        }
        throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
    }

    /**
     * @param parameter the parameter
     * @return the name
     * @morphia.internal
     */
    @MorphiaInternal
    public static String getParameterName(Parameter parameter) {
        Name name = parameter.getAnnotation(Name.class);
        return name != null ? name.value() : parameter.getName();
    }

    private static boolean namesMatchProperties(EntityModel model, Constructor<?> constructor) {
        for (Parameter parameter : constructor.getParameters()) {
            if (model.getProperty(getParameterName(parameter)) == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void set(@Nullable Object value, PropertyModel model) {
        if (instance != null) {
            model.setValue(instance, value);
        } else if (plan.types.length == 0) {
            // a no-arg constructor needs none of the values, so construct now and set each value as it arrives instead
            // of buffering them all
            model.setValue(getInstance(), value);
        } else {
            Integer position = plan.positions.get(model.getName());
            if (position != null) {
                parameters[position] = plan.boxedTypes[position].isInstance(value)
                        ? value
                        : widen(conversions.convert(value, plan.types[position]), plan.types[position]);
            } else {
                // only properties the constructor doesn't take need setting after construction. re-setting the others would
                // overwrite whatever the constructor did with the value and, for final fields, write them reflectively (JEP 500).
                if (pendingModels == null) {
                    pendingModels = new ArrayList<>();
                    pendingValues = new ArrayList<>();
                }
                pendingModels.add(model);
                pendingValues.add(value);
            }
        }
    }

    /**
     * Widens a boxed number for a primitive parameter to its wrapper type, as {@link Constructor#newInstance} would, so that
     * an instantiator gets the exact wrapper type. Anything else is returned as is, for the instantiator to reject.
     */
    @Nullable
    private static Object widen(@Nullable Object value, Class<?> type) {
        Class<?> boxedType = Conversions.boxedType(type);
        int to = type.isPrimitive() ? WIDENING.indexOf(boxedType) : -1;
        if (to < 0 || boxedType.isInstance(value)) {
            return value;
        }
        Number number = value instanceof Character c ? Integer.valueOf(c) : value instanceof Number n ? n : null;
        // a char widens to an int or anything an int widens to, but not to a short
        int from = value instanceof Character ? WIDENING.indexOf(Short.class) : number != null ? WIDENING.indexOf(number.getClass()) : -1;
        if (from < 0 || from >= to) {
            return value;
        }
        return switch (to) {
            case 1 -> number.shortValue();
            case 2 -> number.intValue();
            case 3 -> number.longValue();
            case 4 -> number.floatValue();
            default -> number.doubleValue();
        };
    }

    /**
     * @return a primitive type's zero as its exact wrapper type, or null for any other type
     */
    @Nullable
    private static Object zeroValue(Class<?> type) {
        return type.isPrimitive() ? Array.get(Array.newInstance(type, 1), 0) : null;
    }
}
