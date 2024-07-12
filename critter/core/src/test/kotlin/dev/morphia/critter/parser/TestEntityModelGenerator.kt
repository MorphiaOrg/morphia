package dev.morphia.critter.parser

import dev.morphia.critter.CritterEntityModel
import dev.morphia.critter.parser.generators.CritterEntityModelGenerator
import dev.morphia.critter.parser.generators.Generators
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.Mapper
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.NoInjection
import org.testng.annotations.Test

class TestEntityModelGenerator {
    companion object {
        init {
            critterClassLoader.debug = true
        }
    }

    val entityModel: CritterEntityModel
    val control: CritterEntityModel
    val mapper = Mapper(Generators.config)

    init {
        var generator = CritterEntityModelGenerator(Example::class.java)
        val className = generator.generatedType.className
        critterClassLoader.register(className, generator.emit())
        entityModel =
            critterClassLoader
                .loadClass(className)
                .getConstructor(Mapper::class.java)
                .newInstance(mapper) as CritterEntityModel
        control =
            critterClassLoader
                .loadClass("dev.morphia.critter.sources.ExampleEntityModel")
                .getConstructor(Mapper::class.java)
                .newInstance(mapper) as CritterEntityModel
    }

    @Test(dataProvider = "methods")
    fun testEntityModel(name: String, @NoInjection method: Method) {
        method.trySetAccessible()
        val expected = method.invoke(control)
        val actual = method.invoke(entityModel)
        assertEquals(actual, expected, "${method.name} should return the same value")
    }

    @DataProvider(name = "methods")
    fun methods(): Array<Array<Any>> {
        val array =
            CritterEntityModel::class
                .java
                .methods
                .filterNot { method -> Modifier.isFinal(method.modifiers) }
                .filter { method -> method.parameterCount == 0 }
                .filter { method -> method.declaringClass == CritterEntityModel::class.java }
                .map { arrayOf(it.name, it) }
                .toTypedArray()
        return array
    }
}
