package dev.morphia.critter.parser

import dev.morphia.critter.parser.generators.EntityAccessorGenerator
import dev.morphia.critter.parser.generators.EntityTypeUpdate
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.DummyEntity
import dev.morphia.critter.sources.KotlinDummyEntity
import ksp.org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.className
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
    fun testNestedClass(type: Class<*>) {
        val critterClassLoader = CritterClassLoader(Thread.currentThread().contextClassLoader)
        val bytes =
            EntityTypeUpdate(type.name)
                .update(mapOf("name" to String::class.java, "age" to Int::class.java))
        critterClassLoader.register(type.name, bytes)

        critterClassLoader.dump("target")

        val entity = critterClassLoader.loadClass(type.name).getConstructor().newInstance()

        testStringType(type, critterClassLoader, entity)
        testPrimitiveType(type, critterClassLoader, entity)
    }

    private fun testStringType(
        type: Class<*>,
        critterClassLoader: CritterClassLoader,
        entity: Any,
    ) {
        val generator = EntityAccessorGenerator(type, "name", String::class.java)
        critterClassLoader.register(generator.accessorType.className, generator.dump())

        critterClassLoader.dump("target")
        if (EARLY) return
        val type = generator.accessorType.className
        var accessor =
            (critterClassLoader.loadClass(type) as Class<PropertyAccessor<String>>)
                .getConstructor()
                .newInstance()

        accessor.set(entity, "set externally")

        assertEquals(accessor.get(entity), "set externally")
        assertTrue(entity.toString().contains("set externally"), entity.toString())
    }

    private fun testPrimitiveType(
        type: Class<*>,
        critterClassLoader: CritterClassLoader,
        entity: Any,
    ) {
        val generator = EntityAccessorGenerator(type, "age", Int::class.java)
        critterClassLoader.register(generator.accessorType.className, generator.dump())

        critterClassLoader.dump("target")
        if (EARLY) return
        val type = generator.accessorType.className
        var accessor =
            (critterClassLoader.loadClass(type) as Class<PropertyAccessor<Int>>)
                .getConstructor()
                .newInstance()

        accessor.set(entity, 100)

        assertEquals(accessor.get(entity), 100)
        assertTrue(entity.toString().contains("100"), entity.toString())
    }

    @DataProvider(name = "classes")
    fun names(): Array<Class<out Any>> {
        return arrayOf(DummyEntity::class.java, KotlinDummyEntity::class.java)
    }
}
