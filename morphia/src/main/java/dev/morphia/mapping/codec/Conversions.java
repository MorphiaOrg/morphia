package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;

/**
 * Defines basic type conversions
 */
public final class Conversions {
    private static final Logger LOG = LoggerFactory.getLogger(Conversions.class);

    private static Map<Class<?>, Map<Class<?>, Function<?, ?>>> conversions = new HashMap<>();

    static {
        registerStringConversions();

        register(Binary.class, byte[].class, Binary::getData);

        register(Integer.class, Byte.class, Integer::byteValue);

        register(Double.class, Long.class, Double::longValue, "Converting a double value to a long.  Possible loss of precision.");
        register(Double.class, Integer.class, Double::intValue, "Converting a double value to an int.  Possible loss of precision.");
        register(Double.class, Float.class, Double::floatValue, "Converting a double value to a float.  Possible loss of precision.");

        register(Long.class, Double.class, Long::doubleValue);
        register(Long.class, Float.class, Long::floatValue);

        register(Float.class, Long.class, Float::longValue, "Converting a float value to a long.  Possible loss of precision.");
        register(Float.class, Integer.class, Float::intValue, "Converting a float value to an int.  Possible loss of precision.");

        register(URI.class, String.class, u -> {
            try {
                return u.toURL().toExternalForm().replace(".", "%46");
            } catch (MalformedURLException e) {
                throw new MappingException("Could not convert URI: " + u);
            }
        });
    }

    private Conversions() {
    }

    private static void registerStringConversions() {
        register(String.class, ObjectId.class, ObjectId::new);
        register(String.class, Character.class, s -> {
            if (s.length() == 1) {
                return s.charAt(0);
            } else if (s.isEmpty()) {
                return (char) 0;
            } else {
                throw new MappingException("Could not convert String to char: " + s);
            }
        });
        register(String.class, Boolean.class, Boolean::parseBoolean);
        register(String.class, Byte.class, Byte::parseByte);
        register(String.class, Double.class, Double::parseDouble);
        register(String.class, Integer.class, Integer::parseInt);
        register(String.class, Long.class, Long::parseLong);
        register(String.class, Float.class, Float::parseFloat);
        register(String.class, Short.class, Short::parseShort);
        register(String.class, URI.class, str -> URI.create(str.replace("%46", ".")));
    }

    private static <F, T> void register(final Class<F> fromType, final Class<T> toType, final Function<F, T> function) {
        register(fromType, toType, function, null);
    }

    private static <F, T> void register(final Class<F> fromType, final Class<T> toType, final Function<F, T> function,
                                        final String warning) {
        final Function<F, T> conversion = warning == null
                                          ? function
                                          : f -> {
                                              if (LOG.isWarnEnabled()) {
                                                  LOG.warn(warning);
                                              }
                                              return function.apply(f);
                                          };
        conversions.computeIfAbsent(fromType, (Class<?> c) -> new HashMap<>())
                   .put(toType, conversion);
    }

    /**
     * Attempts to convert a value to the given type
     *
     * @param value  the value to convert
     * @param target the target type
     * @return the potentially converted value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object convert(final Object value, final Class<?> target) {
        if (value == null) {
            return convertNull(target);
        }

        final Class<?> fromType = value.getClass();
        if (fromType.equals(target)) {
            return value;
        }

        final Function function = conversions
                                      .computeIfAbsent(fromType, (f) -> new HashMap<>())
                                      .get(target);
        if (function == null) {
            if (target.equals(String.class)) {
                return value.toString();
            }
            if (target.isEnum() && fromType.equals(String.class)) {
                return Enum.valueOf((Class<? extends Enum>) target, (String) value);
            }
            return value;
        }
        return function.apply(value);
    }

    private static Object convertNull(final Class<?> toType) {
        if (isNumber(toType)) {
            return 0;
        } else if (isBoolean(toType)) {
            return FALSE;
        }
        return null;
    }

    private static boolean isNumber(final Class<?> type) {
        return type.isPrimitive() && !type.equals(boolean.class);
    }

    private static boolean isBoolean(final Class<?> type) {
        return type.equals(boolean.class);
    }
}
