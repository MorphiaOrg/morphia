package dev.morphia.mapping.experimental;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class ConstructorlessInstance {

    private static final Map<Class<?>, Object> DEFAULT_VALUES = Stream
            .of(boolean.class, byte.class, char.class, double.class, float.class, int.class, long.class, short.class)
            .collect(toMap(clazz -> (Class<?>) clazz, clazz -> Array.get(Array.newInstance(clazz, 1), 0)));


    public static <T> T create(final Class<? super T> rawType) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        var constructor = Arrays.stream(rawType.getDeclaredConstructors()).min(Comparator.comparingInt(Constructor::getParameterCount)).orElseThrow(() -> new IllegalStateException("Found no constructors"));
        var arguments = new Object[constructor.getParameterCount()];
        createDefaultArguments(arguments, constructor.getParameters());
        constructor.setAccessible(true);

        //noinspection unchecked
        return (T) constructor.newInstance(arguments);
    }

    private static void createDefaultArguments(Object[] arguments, Parameter[] parameters) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        for (int i = 0; i < parameters.length; i++) {
            arguments[i] = createDefaultArgument(parameters[i]);
        }
    }

    private static Object createDefaultArgument(Parameter parameter) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        var type = parameter.getType();
        if (type.isPrimitive()) {
            return DEFAULT_VALUES.get(type);
        }

        if (type.isAssignableFrom(String.class)) {
            return "";
        }

        return create(type);
    }
}
