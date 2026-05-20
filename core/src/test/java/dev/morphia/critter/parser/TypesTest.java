package dev.morphia.critter.parser;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import dev.morphia.critter.parser.gizmo.GizmoExtensions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.Type;

public class TypesTest {

    static Stream<Arguments> types() {
        return Stream.of(
                // Primitive types
                Arguments.of(boolean.class),
                Arguments.of(char.class),
                Arguments.of(byte.class),
                Arguments.of(short.class),
                Arguments.of(int.class),
                Arguments.of(float.class),
                Arguments.of(long.class),
                Arguments.of(double.class),

                // Object types
                Arguments.of(String.class),
                Arguments.of(Locale.class),
                Arguments.of(Date.class),
                Arguments.of(UUID.class),
                Arguments.of(BigDecimal.class),
                Arguments.of(Instant.class),

                // Arrays of boxed primitives (Kotlin Array<X> = Java boxed X[])
                Arguments.of(Boolean[].class),
                Arguments.of(Character[].class),
                Arguments.of(Byte[].class),
                Arguments.of(Short[].class),
                Arguments.of(Integer[].class),
                Arguments.of(Float[].class),
                Arguments.of(Long[].class),
                Arguments.of(Double[].class),

                // Primitive arrays (Kotlin XArray = Java primitive x[])
                Arguments.of(boolean[].class),
                Arguments.of(char[].class),
                Arguments.of(byte[].class),
                Arguments.of(short[].class),
                Arguments.of(int[].class),
                Arguments.of(float[].class),
                Arguments.of(long[].class),
                Arguments.of(double[].class),

                // 2D primitive arrays (Kotlin Array<XArray> = Java primitive x[][])
                Arguments.of(boolean[][].class),
                Arguments.of(int[][].class),

                // Arrays of objects
                Arguments.of(String[].class),
                Arguments.of(Locale[].class),
                Arguments.of(Date[].class),
                Arguments.of(UUID[].class),
                Arguments.of(BigDecimal[].class),
                Arguments.of(Instant[].class),

                // Arrays of arrays (2D)
                Arguments.of(Boolean[][].class),
                Arguments.of(Integer[][].class),
                Arguments.of(String[][].class),
                Arguments.of(Locale[][].class),

                // Arrays of arrays of arrays (3D)
                Arguments.of(Integer[][][].class),
                Arguments.of(String[][][].class));
    }

    @ParameterizedTest
    @MethodSource("types")
    public void asClassConversion(Class<?> expected) {
        Type type = Type.getType(expected);
        Class<?> actual = GizmoExtensions.asClass(type, Thread.currentThread().getContextClassLoader());
        Assertions.assertEquals(expected, actual, "Type " + type.getDescriptor() + " should convert to " + expected.getName());
    }
}
