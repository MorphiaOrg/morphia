package dev.morphia.critter.parser

import dev.morphia.critter.parser.generators.EntityAccessorGenerator
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import org.bson.codecs.pojo.PropertyAccessor
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class TestAccessorsMutators : BaseCritterTest() {
    @Test(dataProvider = "classes")
    fun testPropertyAccessors(type: Class<*>) {
        val testFields =
            listOf(
                listOf("name", String::class.java, "set externally"),
                listOf("age", Int::class.java, 100),
                listOf("salary", java.lang.Long::class.java, 100_000L)
            )

        val entity = critterClassLoader.loadClass(type.name).getConstructor().newInstance()

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
        type: Class<*>,
        critterClassLoader: CritterClassLoader,
        entity: Any,
        fieldName: String,
        fieldType: Class<*>,
        testValue: Any,
    ) {
        val generator = EntityAccessorGenerator(type, fieldName, fieldType)
        critterClassLoader.register(generator.generatedType.className, generator.emit())

        val accessor =
            (critterClassLoader.loadClass(generator.generatedType.className)
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
    fun names(): Array<Class<out Any>> {
        return arrayOf(Example::class.java /*, KotlinDummyEntity::class.java*/)
    }
}
