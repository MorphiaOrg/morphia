package dev.morphia.critter.parser

import dev.morphia.critter.CritterEntityModel
import dev.morphia.critter.parser.GeneratorTest.methodNames
import dev.morphia.critter.parser.generators.CritterEntityModelGenerator
import dev.morphia.critter.parser.generators.CritterPropertyModelGenerator
import dev.morphia.critter.parser.generators.Generators
import dev.morphia.critter.parser.java.CritterClassLoader
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import java.lang.reflect.Method
import org.objectweb.asm.Type
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.NoInjection
import org.testng.annotations.Test

class TestPropertyModelGenerator {
    companion object {
        init {
            CritterClassLoader.debug = true
        }
    }

    val entityModel: CritterEntityModel
    val mapper = Mapper(Generators.config)

    init {
        val generator = CritterEntityModelGenerator(Example::class.java)
        val className = generator.generatedType.className
        critterClassLoader.register(className, generator.emit())
        entityModel =
            critterClassLoader
                .loadClass(className)
                .getConstructor(Mapper::class.java)
                .newInstance(mapper) as CritterEntityModel
    }

    @Test(dataProvider = "properties")
    fun testProperty(control: PropertyModel, methodName: String, @NoInjection method: Method) {
        val propertyModel = generateModel(control)

        println(
            "exampleModel = [${control.name}], methodName = [${methodName}], method = [${method}]"
        )
        val expected = method.invoke(control)
        val actual = method.invoke(propertyModel)
        assertEquals(actual, expected, "${method.name} should return the same value")
    }

    private fun generateModel(control: PropertyModel): CritterPropertyModel {
        val generator = CritterPropertyModelGenerator(Example::class.java, control.name)
        val className = generator.generatedType.className
        critterClassLoader.register(className, generator.emit())
        critterClassLoader.dump(
            control::class.java.name,
            mapOf(Type.getType(control::class.java) to generator.generatedType)
        )

        return critterClassLoader
            .loadClass(className)
            .getConstructor(EntityModel::class.java)
            .newInstance(entityModel) as CritterPropertyModel
    }

    @DataProvider(name = "properties")
    fun methods(): Array<Array<Any>> {
        val methods = methodNames(CritterPropertyModel::class.java)
        return listOf("dev.morphia.critter.sources.ExampleNamePropertyModel")
            .map { loadModel(it) }
            .flatMap { propertyModel ->
                methods.map { method -> arrayOf(propertyModel, method[0], method[1]) }
            }
            .toTypedArray()
    }

    private fun loadModel(type: String): PropertyModel {
        return critterClassLoader
            .loadClass(type)
            .getConstructor(EntityModel::class.java)
            .newInstance(entityModel) as PropertyModel
    }
}
