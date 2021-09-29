package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.MappingException;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

    private static final Map<Class<?>, Map<Class<?>, Function<?, ?>>> CONVERSIONS = new HashMap<>();

    static {
        registerStringConversions();

        register(Binary.class, byte[].class, Binary::getData);

        register(Date.class, Long.class, Date::getTime);
        register(Date.class, long.class, Date::getTime);

        register(Instant.class, Long.class, Instant::toEpochMilli);
        register(Instant.class, long.class, Instant::toEpochMilli);

        register(Double.class, Long.class, Double::longValue, "Converting a double value to a long.  Possible loss of precision.");
        register(double.class, Long.class, Double::longValue, "Converting a double value to a long.  Possible loss of precision.");
        register(Double.class, long.class, Double::longValue, "Converting a double value to a long.  Possible loss of precision.");
        register(double.class, long.class, Double::longValue, "Converting a double value to a long.  Possible loss of precision.");
        register(Double.class, Integer.class, Double::intValue, "Converting a double value to an int.  Possible loss of precision.");
        register(double.class, Integer.class, Double::intValue, "Converting a double value to an int.  Possible loss of precision.");
        register(Double.class, int.class, Double::intValue, "Converting a double value to an int.  Possible loss of precision.");
        register(double.class, int.class, Double::intValue, "Converting a double value to an int.  Possible loss of precision.");
        register(Double.class, Float.class, Double::floatValue, "Converting a double value to a float.  Possible loss of precision.");
        register(double.class, Float.class, Double::floatValue, "Converting a double value to a float.  Possible loss of precision.");
        register(Double.class, float.class, Double::floatValue, "Converting a double value to a float.  Possible loss of precision.");
        register(double.class, float.class, Double::floatValue, "Converting a double value to a float.  Possible loss of precision.");

        register(Integer.class, Byte.class, Integer::byteValue);
        register(int.class, Byte.class, Integer::byteValue);
        register(Integer.class, byte.class, Integer::byteValue);
        register(int.class, byte.class, Integer::byteValue);

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
        register(String.class, BigDecimal.class, BigDecimal::new);
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
        register(String.class, Integer.class, Integer::valueOf);
        register(String.class, Long.class, Long::parseLong);
        register(String.class, Float.class, Float::parseFloat);
        register(String.class, Short.class, Short::parseShort);
        register(String.class, URI.class, str -> URI.create(str.replace("%46", ".")));
        register(String.class, UUID.class, UUID::fromString);
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
    public static <S, T> void register(Class<S> source, Class<T> target, Function<S, T> function) {
        register(source, target, function, null);
    }

    /**
     * Attempts to convert a value to the given type
     *
     * @param value  the value to convert
     * @param target the target type
     * @param <T>    the target type
     * @return the potentially converted value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    public static <T> T convert(@Nullable Object value, Class<T> target) {
        if (value == null) {
            return (T) convertNull(target);
        }

        final Class<?> fromType = value.getClass();
        if (fromType.equals(target)) {
            return (T) value;
        }

        final Function function = CONVERSIONS
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

    @Nullable
    private static Object convertNull(Class<?> toType) {
        if (isNumber(toType)) {
            return 0;
        } else if (isBoolean(toType)) {
            return FALSE;
        }
        return null;
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
    public static <S, T> void register(Class<S> source, Class<T> target, Function<S, T> function,
                                       @Nullable String warning) {
        final Function<S, T> conversion = warning == null
                                          ? function
                                          : s -> {
                                              if (LOG.isWarnEnabled()) {
                                                  LOG.warn(warning);
                                              }
                                              return function.apply(s);
                                          };
        CONVERSIONS.computeIfAbsent(source, (Class<?> c) -> new HashMap<>())
                   .put(target, conversion);
    }

    private static boolean isNumber(Class<?> type) {
        return type.isPrimitive() && !type.equals(boolean.class);
    }

    private static boolean isBoolean(Class<?> type) {
        return type.equals(boolean.class);
    }
}
