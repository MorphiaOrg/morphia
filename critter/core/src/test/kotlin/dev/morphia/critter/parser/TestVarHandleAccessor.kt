package dev.morphia.critter.parser

import dev.morphia.critter.Critter.Companion.critterPackage
import dev.morphia.critter.CritterClassLoader
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator
import dev.morphia.critter.sources.Example
import dev.morphia.critter.titleCase
import org.bson.codecs.pojo.PropertyAccessor
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNull
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class TestVarHandleAccessor {
    private lateinit var classLoader: CritterClassLoader

    @BeforeClass
    fun setup() {
        classLoader = CritterClassLoader()
        CritterGizmoGenerator.generate(Example::class.java, classLoader, runtimeMode = true)
    }

    @Test
    fun testEntityNotModified() {
        // In runtime mode the entity class must NOT have synthetic __readXxx/__writeXxx methods
        // (as opposed to the __readXxxTemplate methods that exist in the source)
        val methods = Example::class.java.declaredMethods.map { it.name }
        val syntheticRead = methods.filter { it.startsWith("__read") && !it.endsWith("Template") }
        val syntheticWrite = methods.filter { it.startsWith("__write") && !it.endsWith("Template") }
        assert(syntheticRead.isEmpty()) {
            "Entity class should not have synthetic __read methods but found: $syntheticRead"
        }
        assert(syntheticWrite.isEmpty()) {
            "Entity class should not have synthetic __write methods but found: $syntheticWrite"
        }
    }

    @Test
    fun testStringField() {
        val entity = Example()
        val accessor = loadAccessor<String>(Example::class.java, "name")

        assertNull(accessor.get(entity))
        accessor.set(entity, "hello")
        assertEquals(accessor.get(entity), "hello")
    }

    @Test
    fun testIntPrimitiveField() {
        val entity = Example()
        val accessor = loadAccessor<Any>(Example::class.java, "age")

        // Default value is 21 (set in field initializer)
        assertEquals(accessor.get(entity), 21)
        accessor.set(entity, 42)
        assertEquals(accessor.get(entity), 42)
    }

    @Test
    fun testLongBoxedField() {
        val entity = Example()
        val accessor = loadAccessor<Any>(Example::class.java, "salary")

        // Default value is 2L (set in field initializer)
        assertEquals(accessor.get(entity), 2L)
        accessor.set(entity, 100_000L)
        assertEquals(accessor.get(entity), 100_000L)
    }

    @Test
    fun testAccessorsInstantiatable() {
        // All three generated accessor classes must be loadable with no-arg constructor
        listOf("name", "age", "salary").forEach { field ->
            val cls =
                classLoader.loadClass(
                    "${critterPackage(Example::class.java)}.${field.titleCase()}Accessor"
                )
            cls.getConstructor().newInstance()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> loadAccessor(entityType: Class<*>, fieldName: String): PropertyAccessor<T> {
        val accessorClass =
            classLoader.loadClass("${critterPackage(entityType)}.${fieldName.titleCase()}Accessor")
                as Class<PropertyAccessor<T>>
        return accessorClass.getConstructor().newInstance()
    }
}
