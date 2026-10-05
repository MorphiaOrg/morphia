package dev.morphia.mapping.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
    private final Plan plan;
    private final Conversions conversions;
    private final Object[] parameters;
    private final List<PropertyModel> pendingModels = new ArrayList<>();
    private final List<Object> pendingValues = new ArrayList<>();
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
        this.parameters = plan.zeroValues.clone();
    }

    /**
     * The per-constructor work, done once per model rather than once per decoded instance.
     */
    public static final class Plan {
        private final EntityModel model;
        private final Constructor<?> constructor;
        private final Map<String, Integer> positions = new LinkedHashMap<>();
        private final Class<?>[] types;
        private final Object[] zeroValues;

        /**
         * @param model       the model
         * @param constructor the constructor to use
         */
        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Plan(EntityModel model, Constructor<?> constructor) {
            this.model = model;
            this.constructor = constructor;
            this.constructor.setAccessible(true);
            final Parameter[] constructorParameters = constructor.getParameters();
            types = new Class<?>[constructorParameters.length];
            zeroValues = new Object[constructorParameters.length];
            for (int i = 0; i < constructorParameters.length; i++) {
                final Parameter parameter = constructorParameters[i];
                types[i] = parameter.getType();
                zeroValues[i] = zeroValue(parameter);
                String name = getParameterName(parameter);
                if (name.matches("arg[0-9]+")) {
                    throw new MappingException(Sofia.unnamedConstructorParameter(model.getType().getName()));
                }
                if (positions.put(name, i) != null) {
                    throw new MappingException(Sofia.duplicatedParameterName(model.getType().getName(), name));
                }
            }
        }
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

        return constructors.stream()
                .filter(it -> stream(it.getParameters())
                        .allMatch(param -> Objects.equals(propertyMap.get(getParameterName(param)), param.getType())))
                .sorted((o1, o2) -> compare(o2.getParameterCount(), o1.getParameterCount()))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasLifecycleEvents(EntityModel model) {
        return model.hasLifecycle(PreLoad.class)
                || model.hasLifecycle(PostLoad.class)
                || model.hasLifecycle(PrePersist.class)
                || model.hasLifecycle(PostPersist.class);
    }

    @Override
    public Object getInstance() {
        if (instance == null) {
            try {
                instance = plan.constructor.newInstance(parameters);
                for (int i = 0; i < pendingModels.size(); i++) {
                    pendingModels.get(i).setValue(instance, pendingValues.get(i));
                }
            } catch (Exception e) {
                throw new MappingException(Sofia.cannotInstantiate(plan.model.getType().getName(), e.getMessage()), e);
            }
        }
        return instance;
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
        } else {
            Integer position = plan.positions.get(model.getName());
            if (position != null) {
                parameters[position] = conversions.convert(value, plan.types[position]);
            } else {
                // only properties the constructor doesn't take need setting after construction. re-setting the others would
                // overwrite whatever the constructor did with the value and, for final fields, write them reflectively (JEP 500).
                pendingModels.add(model);
                pendingValues.add(value);
            }
        }
    }

    @Nullable
    private static Object zeroValue(Parameter parameter) {
        if (!parameter.getType().isPrimitive()) {
            return null;
        } else if (parameter.getType().equals(boolean.class)) {
            return false;
        } else {
            return 0;
        }
    }
}
