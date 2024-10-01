package dev.morphia.critter.parser

import dev.morphia.critter.Critter.Companion.critterClassLoader
import dev.morphia.critter.parser.GeneratorTest.entityModel
import dev.morphia.critter.parser.GeneratorTest.methodNames
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel
import java.lang.reflect.Method
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.NoInjection

class TestEntityModelGenerator {
    val control: CritterEntityModel
    val mapper = Mapper(Generators.config)

    init {
        control =
            critterClassLoader
                .loadClass("dev.morphia.critter.sources.ExampleEntityModelTemplate")
                .getConstructor(Mapper::class.java)
                .newInstance(mapper) as CritterEntityModel
        critterClassLoader.dump("dev.morphia.critter.sources.ExampleEntityModelTemplate")
    }

    //    @Test(dataProvider = "methods")
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
