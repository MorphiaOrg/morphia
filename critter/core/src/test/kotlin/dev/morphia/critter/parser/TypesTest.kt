package dev.morphia.critter.parser

import dev.morphia.critter.parser.Generators.asClass
import java.math.BigDecimal
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.objectweb.asm.Type
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class TypesTest {
    @DataProvider(name = "types")
    fun typeProvider(): Array<Array<Any>> {
        return arrayOf(
            // Primitive types
            arrayOf(Boolean::class.java),
            arrayOf(Char::class.java),
            arrayOf(Byte::class.java),
            arrayOf(Short::class.java),
            arrayOf(Int::class.java),
            arrayOf(Float::class.java),
            arrayOf(Long::class.java),
            arrayOf(Double::class.java),

            // Object types
            arrayOf(String::class.java),
            arrayOf(Locale::class.java),
            arrayOf(Date::class.java),
            arrayOf(UUID::class.java),
            arrayOf(BigDecimal::class.java),
            arrayOf(Instant::class.java),

            // Arrays of boxed primitives
            arrayOf(Array<Boolean>::class.java),
            arrayOf(Array<Char>::class.java),
            arrayOf(Array<Byte>::class.java),
            arrayOf(Array<Short>::class.java),
            arrayOf(Array<Int>::class.java),
            arrayOf(Array<Float>::class.java),
            arrayOf(Array<Long>::class.java),
            arrayOf(Array<Double>::class.java),

            // Primitive arrays
            arrayOf(BooleanArray::class.java),
            arrayOf(CharArray::class.java),
            arrayOf(ByteArray::class.java),
            arrayOf(ShortArray::class.java),
            arrayOf(IntArray::class.java),
            arrayOf(FloatArray::class.java),
            arrayOf(LongArray::class.java),
            arrayOf(DoubleArray::class.java),

            // 2D primitive arrays
            arrayOf(Array<BooleanArray>::class.java),
            arrayOf(Array<IntArray>::class.java),

            // Arrays of objects
            arrayOf(Array<String>::class.java),
            arrayOf(Array<Locale>::class.java),
            arrayOf(Array<Date>::class.java),
            arrayOf(Array<UUID>::class.java),
            arrayOf(Array<BigDecimal>::class.java),
            arrayOf(Array<Instant>::class.java),

            // Arrays of arrays (2D)
            arrayOf(Array<Array<Boolean>>::class.java),
            arrayOf(Array<Array<Int>>::class.java),
            arrayOf(Array<Array<String>>::class.java),
            arrayOf(Array<Array<Locale>>::class.java),

            // Arrays of arrays of arrays (3D)
            arrayOf(Array<Array<Array<Int>>>::class.java),
            arrayOf(Array<Array<Array<String>>>::class.java),
        )
    }

    @Test(dataProvider = "types")
    fun asClassConversion(expected: Class<*>) {
        val type = Type.getType(expected)
        val actual = type.asClass()

        assertEquals(actual, expected, "Type ${type.descriptor} should convert to ${expected.name}")
    }
}
