package dev.morphia.critter.parser

import dev.morphia.critter.Critter.Companion.critterClassLoader
import dev.morphia.critter.parser.GeneratorTest.entityModel
import dev.morphia.critter.parser.GeneratorTest.methodNames
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import java.lang.reflect.Method
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.NoInjection

class TestPropertyModelGenerator : BaseCritterTest() {
    //    @Test(dataProvider = "properties", testName = "")
    fun testProperty(control: String, methodName: String, @NoInjection method: Method) {
        val propertyModel = getModel(control)

        println("exampleModel = [${control}], methodName = [${methodName}], method = [${method}]")
        val expected = method.invoke(control)
        val actual = method.invoke(propertyModel)
        assertEquals(actual, expected, "${method.name} should return the same value")
    }

    private fun getModel(name: String): CritterPropertyModel {
        return entityModel.getProperty(name) as CritterPropertyModel
    }

    @DataProvider(name = "properties")
    fun methods(): Array<Array<Any>> {
        val methods = methodNames(CritterPropertyModel::class.java)
        return listOf("dev.morphia.critter.sources.ExampleNamePropertyModelTemplate")
            .map { loadModel(it) }
            .flatMap { propertyModel ->
                methods.map { method -> arrayOf(propertyModel.name, method[0], method[1]) }
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
