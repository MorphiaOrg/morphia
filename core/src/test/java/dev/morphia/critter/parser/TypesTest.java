package dev.morphia.critter.parser;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.objectweb.asm.Type;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TypesTest {

    @DataProvider(name = "types")
    public Object[][] typeProvider() {
        return new Object[][] {
                // Primitive types
                { boolean.class },
                { char.class },
                { byte.class },
                { short.class },
                { int.class },
                { float.class },
                { long.class },
                { double.class },

                // Object types
                { String.class },
                { Locale.class },
                { Date.class },
                { UUID.class },
                { BigDecimal.class },
                { Instant.class },

                // Arrays of boxed primitives (Kotlin Array<X> = Java boxed X[])
                { Boolean[].class },
                { Character[].class },
                { Byte[].class },
                { Short[].class },
                { Integer[].class },
                { Float[].class },
                { Long[].class },
                { Double[].class },

                // Primitive arrays (Kotlin XArray = Java primitive x[])
                { boolean[].class },
                { char[].class },
                { byte[].class },
                { short[].class },
                { int[].class },
                { float[].class },
                { long[].class },
                { double[].class },

                // 2D primitive arrays (Kotlin Array<XArray> = Java primitive x[][])
                { boolean[][].class },
                { int[][].class },

                // Arrays of objects
                { String[].class },
                { Locale[].class },
                { Date[].class },
                { UUID[].class },
                { BigDecimal[].class },
                { Instant[].class },

                // Arrays of arrays (2D)
                { Boolean[][].class },
                { Integer[][].class },
                { String[][].class },
                { Locale[][].class },

                // Arrays of arrays of arrays (3D)
                { Integer[][][].class },
                { String[][][].class },
        };
    }

    @Test(dataProvider = "types")
    public void asClassConversion(Class<?> expected) {
        Type type = Type.getType(expected);
        Class<?> actual = Generators.INSTANCE.asClass(type, Thread.currentThread().getContextClassLoader());
        Assert.assertEquals(actual, expected, "Type " + type.getDescriptor() + " should convert to " + expected.getName());
    }
}
