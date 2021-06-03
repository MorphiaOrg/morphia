package dev.morphia.mapping.experimental;

import com.mongodb.lang.Nullable;
import dev.morphia.annotations.experimental.Name;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Defines a Creator that uses a full constructor to create an instance rather than field injection.  This requires that a class have a
 * constructor that accepts a parameter for each mapped field on the class.
 *
 * @morphia.internal
 */
public class ConstructorCreator implements MorphiaInstanceCreator {
    private final Object[] parameters;
    private final Constructor<?> constructor;
    private final EntityModel model;
    private final Map<String, BiFunction<Object[], Object, Void>> positions = new LinkedHashMap<>();

    /**
     * @param model       the model
     * @param constructor the constructor to use
     */
    public ConstructorCreator(EntityModel model, Constructor<?> constructor) {
        this.model = model;
        this.constructor = constructor;
        this.constructor.setAccessible(true);

        final Parameter[] constructorParameters = this.constructor.getParameters();
        this.parameters = new Object[constructorParameters.length];
        for (int i = 0; i < constructorParameters.length; i++) {
            final Parameter parameter = constructorParameters[i];
            final int finalI = i;
            String name = getParameterName(parameter);
            if (name.matches("arg[0-9]+")) {
                throw new MappingException(Sofia.unnamedConstructorParameter(model.getType().getName()));
            }
            BiFunction<Object[], Object, Void> old = positions.put(name, (Object[] params, Object v) -> {
                params[finalI] = v;
                return null;
            });

            if (old != null) {
                throw new MappingException(Sofia.duplicatedParameterName(model.getType().getName(), name));
            }
        }
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
    public Object getInstance() {
        try {
            return constructor.newInstance(parameters);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(Sofia.cannotInstantiate(model.getType().getName(), e.getMessage()));
        }
    }

    @Override
    public void set(@Nullable Object value, PropertyModel model) {
        positions.get(model.getName()).apply(parameters, value);
    }
}
