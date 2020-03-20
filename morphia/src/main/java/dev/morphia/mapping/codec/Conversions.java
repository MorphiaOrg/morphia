package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;

/**
 * Defines basic type conversions.  This class is mostly intended for internal use only but its methods are public so that when cases
 * arise where certain conversions are missing, users can add their in between releases.  However, this must be done with the understand
 * that, however unlikely, this API is subject to change and any uses might break at some point.
 *
 * @morphia.internal
 */
public final class Conversions {
    private static final Logger LOG = LoggerFactory.getLogger(Conversions.class);

    private static Map<Class<?>, Map<Class<?>, Function<?, ?>>> conversions = new HashMap<>();

    static {
        registerStringConversions();

        register(Binary.class, byte[].class, Binary::getData);

        register(Date.class, Long.class, Date::getTime);
        register(Instant.class, Long.class, Instant::toEpochMilli);
        register(Date.class, long.class, Date::getTime);
        register(Instant.class, long.class, Instant::toEpochMilli);

        register(Double.class, Long.class, Double::longValue, "Converting a double value to a long.  Possible loss of precision.");
        register(Double.class, Integer.class, Double::intValue, "Converting a double value to an int.  Possible loss of precision.");
        register(Double.class, Float.class, Double::floatValue, "Converting a double value to a float.  Possible loss of precision.");

        register(Integer.class, Byte.class, Integer::byteValue);

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

    /**
     * Register a conversion between two types.  For example, to register the conversion of {@link Date} to a {@link Long}, this method
     * could be invoked as follows:
     *
     * <code>
     * register(Date.class, Long.class, Date::getTime);
     * </code>
     *
     * @param source   the source type
     * @param target   the target type
     * @param function the function that performs the conversion.  This is often just a method reference.
     * @param <S>      the source type
     * @param <T>      the target type.
     */
    public static <S, T> void register(final Class<S> source, final Class<T> target, final Function<S, T> function) {
        register(source, target, function, null);
    }

    /**
     * Register a conversion between two types.  For example, to register the conversion of {@link Date} to a {@link Long}, this method
     * could be invoked as follows:
     *
     * <code>
     * register(Date.class, Long.class, Date::getTime);
     * </code>
     *
     * @param source   the source type
     * @param target   the target type
     * @param function the function that performs the conversion.  This is often just a method reference.
     * @param warning  if non-null, this will be the message logged on the WARN level indicating the conversion is taking place.
     * @param <S>      the source type
     * @param <T>      the target type.
     */
    public static <S, T> void register(final Class<S> source, final Class<T> target, final Function<S, T> function,
                                       final String warning) {
        final Function<S, T> conversion = warning == null
                                          ? function
                                          : s -> {
                                              if (LOG.isWarnEnabled()) {
                                                  LOG.warn(warning);
                                              }
                                              return function.apply(s);
                                          };
        conversions.computeIfAbsent(source, (Class<?> c) -> new HashMap<>())
                   .put(target, conversion);
    }

    /**
     * Attempts to convert a value to the given type
     *
     * @param value  the value to convert
     * @param target the target type
     * @return the potentially converted value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T convert(final Object value, final Class<T> target) {
        if (value == null) {
            return (T) convertNull(target);
        }

        final Class<?> fromType = value.getClass();
        if (fromType.equals(target)) {
            return (T) value;
        }

        final Function function = conversions
                                      .computeIfAbsent(fromType, (f) -> new HashMap<>())
                                      .get(target);
        if (function == null) {
            if (target.equals(String.class)) {
                return (T) value.toString();
            }
            if (target.isEnum() && fromType.equals(String.class)) {
                return (T) Enum.valueOf((Class<? extends Enum>) target, (String) value);
            }
            return (T) value;
        }
        return (T) function.apply(value);
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
