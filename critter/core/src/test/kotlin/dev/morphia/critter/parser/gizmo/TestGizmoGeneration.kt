package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.CritterGizmoGenerator
import dev.morphia.critter.parser.GeneratorTest
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import io.quarkus.gizmo.ClassOutput
import org.bson.codecs.pojo.PropertyAccessor
import org.testng.annotations.Test

class TestGizmoGeneration {
    private val classOutput = ClassOutput { name, data ->
        critterClassLoader.register(name.replace('/', '.'), data)
    }

    @Test
    fun testGizmo() {
        val generator = CritterGizmoGenerator(critterClassLoader, GeneratorTest.mapper)
        generator.generate(Example::class.java)
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.AgeModel")
        val nameModel =
            critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.NameModel")
        invokeAll(nameModel)
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.SalaryModel")
        critterClassLoader
            .loadClass("dev.morphia.critter.sources.__morphia.example.AgeAccessor")
            .getConstructor()
            .newInstance()
        critterClassLoader
            .loadClass("dev.morphia.critter.sources.__morphia.example.NameAccessor")
            .getConstructor()
            .newInstance()
        critterClassLoader
            .loadClass("dev.morphia.critter.sources.__morphia.example.SalaryAccessor")
            .getConstructor()
            .newInstance()
    }

    private fun invokeAll(nameModel: Class<*>) {
        val instance = nameModel.constructors[0].newInstance(null)
        PropertyAccessor::class.java.declaredMethods.forEach { method ->
            nameModel.getMethod(method.name).invoke(instance)
        }
    }
}
