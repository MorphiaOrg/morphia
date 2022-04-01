package dev.morphia.mapping.internal;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.lang.Integer.compare;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

/**
 * Defines a Creator that uses a full constructor to create an instance rather than field injection.  This requires that a class have a
 * constructor that accepts a parameter for each mapped field on the class.
 *
 * @morphia.internal
 */
@MorphiaInternal
public class ConstructorCreator implements MorphiaInstanceCreator {
    private final Object[] parameters;
    private final Constructor<?> constructor;
    private final EntityModel model;
    private final Map<String, BiFunction<Object[], Object, Void>> positions = new LinkedHashMap<>();
    private final List<Consumer<Object>> setFunctions = new ArrayList<>();
    private Object instance;

    /**
     * @param model       the model
     * @param constructor the constructor to use
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ConstructorCreator(EntityModel model, Constructor<?> constructor) {
        this.model = model;
        this.constructor = constructor;
        this.constructor.setAccessible(true);

        final Parameter[] constructorParameters = this.constructor.getParameters();
        this.parameters = new Object[constructorParameters.length];
        for (int i = 0; i < constructorParameters.length; i++) {
            final Parameter parameter = constructorParameters[i];
            parameters[i] = zeroValue(parameter);
            final int finalI = i;
            String name = getParameterName(parameter);
            if (name.matches("arg[0-9]+")) {
                throw new MappingException(Sofia.unnamedConstructorParameter(model.getType().getName()));
            }
            BiFunction<Object[], Object, Void> old = positions.put(name, (Object[] params, Object v) -> {
                params[finalI] = Conversions.convert(v, parameter.getType());
                return null;
            });

            if (old != null) {
                throw new MappingException(Sofia.duplicatedParameterName(model.getType().getName(), name));
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
                               .orElseThrow(() -> new IllegalStateException("A type with lifecycle events must have a no-arg constructor"));
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
                instance = constructor.newInstance(parameters);
                setFunctions.forEach(function -> function.accept(instance));
            } catch (Exception e) {
                throw new MappingException(Sofia.cannotInstantiate(model.getType().getName(), e.getMessage()), e);
            }
        }
        return instance;
    }

    /**
     * @param model the model to check
     * @return the constructor taking all fields if it exists
     * @morphia.internal
     */
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
            BiFunction<Object[], Object, Void> function = positions.get(model.getName());
            if (function != null) {
                function.apply(parameters, value);
            }
            setFunctions.add((instance) -> {
                model.setValue(instance, value);
            });
        }
    }

    @Nullable
    private Object zeroValue(Parameter parameter) {
        if (!parameter.getType().isPrimitive()) {
            return null;
        } else if (parameter.getType().equals(boolean.class)) {
            return false;
        } else {
            return 0;
        }
    }
}
