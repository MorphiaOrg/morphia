package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.CritterGizmoGenerator
import dev.morphia.critter.parser.GeneratorTest
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.codec.pojo.PropertyModel
import io.quarkus.gizmo.ClassCreator
import io.quarkus.gizmo.ClassOutput
import io.quarkus.gizmo.MethodDescriptor
import java.lang.reflect.Modifier
import org.testng.Assert.assertNotNull
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
        invokeAll(PropertyModel::class.java, nameModel)
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

    private fun invokeAll(type: Class<*>, nameModel: Class<*>) {
        val instance = nameModel.constructors[0].newInstance(null)
        type.declaredMethods
            .filter { it.parameterCount == 0 }
            .forEach { method -> nameModel.getMethod(method.name).invoke(instance) }
    }

    @Test
    fun testConstructors() {
        val className = "dev.morphia.critter.GizmoSubclass"
        val constructorCall =
            ClassCreator.builder()
                .classOutput { name, data ->
                    critterClassLoader.register(name.replace('/', '.'), data)
                }
                .className("dev.morphia.critter.ConstructorCall")
                .build()
        val fieldCreator =
            constructorCall
                .getFieldCreator("name", String::class.java)
                .setModifiers(Modifier.PUBLIC)
        val constructorCreator = constructorCall.getConstructorCreator(String::class.java)
        constructorCreator.invokeSpecialMethod(
            MethodDescriptor.ofConstructor(Object::class.java),
            constructorCreator.`this`
        )
        constructorCreator.setParameterNames(arrayOf("name"))
        constructorCreator.writeInstanceField(
            fieldCreator.fieldDescriptor,
            constructorCreator.`this`,
            constructorCreator.getMethodParam(0)
        )

        constructorCreator.returnVoid()
        constructorCall.close()
        val newInstance =
            critterClassLoader
                .loadClass("dev.morphia.critter.ConstructorCall")
                .getConstructor(String::class.java)
                .newInstance("here i am")

        println("**************** newInstance = ${newInstance}")
        val creator =
            ClassCreator.builder()
                .classOutput { name, data ->
                    critterClassLoader.register(name.replace('/', '.'), data)
                }
                .className(className)
                .superClass("dev.morphia.critter.ConstructorCall")
                .build()
        val constructor = creator.getConstructorCreator(String::class.java)
        constructor.invokeSpecialMethod(
            MethodDescriptor.ofConstructor(
                "dev.morphia.critter.ConstructorCall",
                String::class.java
            ),
            constructor.getThis(),
            constructor.getMethodParam(0)
        )
        constructor.setParameterNames(arrayOf("subName"))
        constructor.returnVoid()
        constructor.close()
        creator.close()
        val instance =
            critterClassLoader
                .loadClass(className)
                .getConstructor(String::class.java)
                .newInstance("This is my name")

        assertNotNull(instance)
    }
}
