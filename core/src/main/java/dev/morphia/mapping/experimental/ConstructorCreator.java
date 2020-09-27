package dev.morphia.mapping.experimental;

import dev.morphia.annotations.experimental.Name;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
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
 * @param <T> the model type
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class ConstructorCreator<T> implements MorphiaInstanceCreator<T> {
    private final Object[] parameters;
    private final Constructor<T> constructor;
    private final EntityModel model;
    private final Map<String, BiFunction<Object[], Object, Void>> positions = new LinkedHashMap<>();

    /**
     * @param model the model
     */
    public ConstructorCreator(EntityModel model) {
        this.model = model;
        constructor = getFullConstructor(model);
        if (constructor == null) {
            throw new MappingException(Sofia.noSuitableConstructor(model.getType()));
        }
        constructor.setAccessible(true);

        final Parameter[] constructorParameters = constructor.getParameters();
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
     * @param <T>   the model type
     * @return the constructor taking all fields if it exists
     * @morphia.internal
     */
    public static <T> Constructor<T> getFullConstructor(EntityModel<T> model) {
        for (Constructor<?> constructor : model.getType().getDeclaredConstructors()) {
            if (constructor.getParameterCount() == model.getFieldModels().size()
                && constructor.getAnnotation(dev.morphia.annotations.experimental.Constructor.class) != null) {
                return (Constructor<T>) constructor;
            }
        }
        return null;
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

    @Override
    public <S> void set(S value, FieldModel<S> model) {
        positions.get(model.getName()).apply(parameters, value);
    }

    @Override
    public T getInstance() {
        try {
            return constructor.newInstance(parameters);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(Sofia.cannotInstantiate(model.getType().getName(), e.getMessage()));
        }
    }
}
