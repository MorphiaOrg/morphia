package dev.morphia.critter.parser

import dev.morphia.critter.parser.generators.AddFieldAccessorMethods
import dev.morphia.critter.parser.generators.EntityAccessorGenerator
import dev.morphia.critter.parser.java.CritterClassLoader
import org.bson.codecs.pojo.PropertyAccessor
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class TestAsmGenerator {
    companion object {
        val EARLY = false
    }

    @Test(dataProvider = "classes")
    fun testPropertyAccessors(type: String) {
        val critterClassLoader = CritterClassLoader(Thread.currentThread().contextClassLoader)
        val testFields =
            listOf(
                listOf("name", String::class.java, "set externally"),
                listOf("age", Int::class.java, 100),
                listOf("salary", java.lang.Long::class.java, 100_000L),
            )
        val bytes =
            AddFieldAccessorMethods(type)
                .update(testFields.map { l -> l[0] as String to l[1] as Class<*> }.toMap())
        critterClassLoader.register(type, bytes)

        critterClassLoader.dump("target")

        val entity = critterClassLoader.loadClass(type).getConstructor().newInstance()

        testFields.forEach { field ->
            testAccessor(
                type,
                critterClassLoader,
                entity,
                field[0] as String,
                field[1] as Class<*>,
                field[2]
            )
        }
    }

    private fun testAccessor(
        type: String,
        critterClassLoader: CritterClassLoader,
        entity: Any,
        fieldName: String,
        fieldType: Class<*>,
        testValue: Any,
    ) {
        val generator = EntityAccessorGenerator(type, fieldName, fieldType)
        critterClassLoader.register(generator.accessorType.className, generator.dump())

        critterClassLoader.dump("target")
        if (EARLY) return
        val accessor =
            (critterClassLoader.loadClass(generator.accessorType.className)
                    as Class<PropertyAccessor<Any>>)
                .getConstructor()
                .newInstance()

        accessor.set(entity, testValue)

        assertEquals(accessor.get(entity), testValue)
        assertTrue(
            entity.toString().contains(testValue.toString()),
            "Could not find '$testValue` in :${entity}"
        )
    }

    @DataProvider(name = "classes")
    fun names(): Array<String> {
        return arrayOf(
            "dev.morphia.critter.sources.DummyEntity",
            "dev.morphia.critter.sources.KotlinDummyEntity"
        )
    }
}
