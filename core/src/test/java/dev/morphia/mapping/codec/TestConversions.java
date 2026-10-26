package dev.morphia.mapping.codec;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConversions {
    @Test
    void boxesPrimitives() {
        Assertions.assertEquals(Integer.class, Conversions.boxedType(int.class));
        Assertions.assertEquals(Boolean.class, Conversions.boxedType(boolean.class));
        Assertions.assertEquals(Character.class, Conversions.boxedType(char.class));
        Assertions.assertEquals(Double.class, Conversions.boxedType(double.class));
        Assertions.assertEquals(List.class, Conversions.boxedType(List.class));
        Assertions.assertEquals(String.class, Conversions.boxedType(String.class));
    }

    @Test
    void convertsWhenAConversionIsRegistered() {
        Conversions conversions = new Conversions(getClass().getClassLoader());

        Assertions.assertEquals(42, conversions.convert("42", int.class));
        Assertions.assertEquals(42L, conversions.convert(42.0, long.class));
        Assertions.assertEquals('c', conversions.convert("c", Character.class));
    }

    @Test
    void passesValuesThroughWhenNoConversionIsRegistered() {
        Conversions conversions = new Conversions(getClass().getClassLoader());
        List<String> list = new ArrayList<>();

        Assertions.assertSame(list, conversions.convert(list, List.class));
        Assertions.assertEquals(7, conversions.convert(7, int.class));
        Assertions.assertEquals(0, conversions.convert(null, int.class));
        Assertions.assertNull(conversions.convert(null, Integer.class));
    }
}
