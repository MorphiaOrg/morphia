package dev.morphia.critter.parser

import dev.morphia.critter.CritterEntityModel
import dev.morphia.critter.parser.GeneratorTest.methodNames
import dev.morphia.critter.parser.generators.CritterEntityModelGenerator
import dev.morphia.critter.parser.generators.Generators
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.Mapper
import java.lang.reflect.Method
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.NoInjection
import org.testng.annotations.Test

class TestEntityModelGenerator {
    companion object {
        init {
            CritterClassLoader.debug = true
        }
    }

    val entityModel: CritterEntityModel
    val control: CritterEntityModel
    val mapper = Mapper(Generators.config)
    val generator: CritterEntityModelGenerator

    init {
        generator = CritterEntityModelGenerator(Example::class.java)
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
        critterClassLoader.dump("dev.morphia.critter.sources.ExampleEntityModel")
    }

    @Test(dataProvider = "methods")
    fun testEntityModel(name: String, @NoInjection method: Method) {
        val expected = method.invoke(control)
        val actual = method.invoke(entityModel)
        assertEquals(actual, expected, "${method.name} should return the same value")
    }

    @DataProvider(name = "methods") fun methods() = methodNames(CritterEntityModel::class.java)
}

fun <String> MutableList<String>.removeWhile(function: (String) -> Boolean): kotlin.String {
    val removed = mutableListOf<String>()
    while (isNotEmpty() && function(first())) {
        removed += removeFirst()
    }

    return (removed + removeFirst()).joinToString("\n")
}
