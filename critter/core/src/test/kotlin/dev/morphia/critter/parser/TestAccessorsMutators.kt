package dev.morphia.critter.parser

import dev.morphia.critter.Critter.Companion.critterClassLoader
import dev.morphia.critter.Critter.Companion.critterPackage
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.critter.titleCase
import org.bson.codecs.pojo.PropertyAccessor
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider

class TestAccessorsMutators : BaseCritterTest() {
    //    @Test(dataProvider = "classes")
    fun testPropertyAccessors(type: Class<*>) {
        val testFields =
            listOf(
                listOf("name", String::class.java, "set externally"),
                listOf("age", Int::class.java, 100),
                listOf("salary", java.lang.Long::class.java, 100_000L)
            )

        val entity = critterClassLoader.loadClass(type.name).getConstructor().newInstance()

        testFields.forEach { field ->
            testAccessor(type, critterClassLoader, entity, field[0] as String, field[2])
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun testAccessor(
        type: Class<*>,
        critterClassLoader: CritterClassLoader,
        entity: Any,
        fieldName: String,
        testValue: Any,
    ) {
        val accessor =
            (critterClassLoader.loadClass(
                    "${critterPackage(type)}${type.simpleName}${fieldName.titleCase()}Accessor"
                ) as Class<PropertyAccessor<Any>>)
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
