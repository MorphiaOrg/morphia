package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.CritterGizmoGenerator
import dev.morphia.critter.parser.GeneratorTest
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import io.quarkus.gizmo.ClassCreator
import io.quarkus.gizmo.ClassOutput
import io.quarkus.gizmo.MethodDescriptor
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
        critterClassLoader.loadClass("dev.morphia.critter.sources.__morphia.example.NameModel")
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

    private fun getAccessor(creator: ClassCreator) {
        val field = creator.getFieldCreator("accessor", PropertyAccessor::class.java)
        val method =
            creator.getMethodCreator(
                MethodDescriptor.ofMethod(
                    creator.className,
                    "getAccessor",
                    PropertyAccessor::class.java.name
                )
            )

        method.returnValue(method.readInstanceField(field.fieldDescriptor, method.`this`))
    }

    private fun ctor(creator: ClassCreator) {
        val constructor = creator.getConstructorCreator(EntityModel::class.java)
        constructor.invokeSpecialMethod(
            MethodDescriptor.ofConstructor(PropertyModel::class.java, EntityModel::class.java),
            constructor.`this`,
            constructor.getMethodParam(0)
        )
        constructor.returnVoid()
    }
}
