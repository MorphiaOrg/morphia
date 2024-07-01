package dev.morphia.critter.parser

import dev.morphia.critter.parser.generators.EntityAccessorGenerator
import dev.morphia.critter.parser.generators.EntityTypeUpdate
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.DummyEntity
import org.bson.codecs.pojo.PropertyAccessor
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

class TestAsmGenerator {
    @Test
    fun testNestedClass() {
        val critterClassLoader = CritterClassLoader(Thread.currentThread().contextClassLoader)
        val bytes =
            EntityTypeUpdate("dev.morphia.critter.sources.DummyEntity")
                .update(mapOf("name" to String::class.java))
        critterClassLoader.register("dev.morphia.critter.sources.DummyEntity", bytes)

        critterClassLoader.dump("target")

        val entity =
            critterClassLoader
                .loadClass("dev.morphia.critter.sources.DummyEntity")
                .getConstructor()
                .newInstance()

        val generator = EntityAccessorGenerator(DummyEntity::class.java, "name", String::class.java)
        critterClassLoader.register(generator.accessorType.className, generator.dump())

        critterClassLoader.dump("target")

        val type = generator.accessorType.className
        var accessor =
            (critterClassLoader.loadClass(type) as Class<PropertyAccessor<String>>)
                .getConstructor()
                .newInstance()

        accessor.set(entity, "set externally")

        assertEquals(accessor.get(entity), "set externally")
        assertTrue(entity.toString().contains("set externally"), entity.toString())
    }
}
