package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.CritterGizmoGenerator
import dev.morphia.critter.parser.GeneratorTest
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.sources.Example
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.TypeData
import io.quarkus.gizmo.ClassCreator
import io.quarkus.gizmo.ClassOutput
import io.quarkus.gizmo.MethodDescriptor
import java.lang.reflect.Modifier
import org.objectweb.asm.Type
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.Assert.fail
import org.testng.annotations.Test

class TestGizmoGeneration {
    private val map: Map<String, Example>? = null
    private val list: List<Map<String, Example>>? = null

    private val classOutput = ClassOutput { name, data ->
        critterClassLoader.register(name.replace('/', '.'), data)
    }

    @Test
    fun testTypeData() {
        var descString = "Ljava/util/Map<Ljava/lang/String;Ldev/morphia/critter/sources/Example;>;"
        var descriptor =
            descriptor(
                Map::class.java,
                descriptor(String::class.java),
                descriptor(Example::class.java)
            )

        assertEquals(descriptor, descString)

        var typeData = descString.typeData()[0]
        assertEquals(
            typeData,
            Map::class.java.typeData(String::class.java.typeData(), Example::class.java.typeData())
        )

        descString =
            "Ljava/util/List<Ljava/util/Map<Ljava/lang/String;Ldev/morphia/critter/sources/Example;>;>;"
        assertEquals(descriptor(List::class.java, descriptor), descString)

        typeData = descString.typeData()[0]
        assertEquals(
            typeData,
            List::class
                .java
                .typeData(
                    Map::class
                        .java
                        .typeData(String::class.java.typeData(), Example::class.java.typeData())
                )
        )

        descriptor =
            descriptor(
                Map::class.java,
                descriptor(String::class.java),
                descriptor(List::class.java, descriptor(Example::class.java))
            )
        println("**************** descString = ${descriptor}")
        typeData = descriptor.typeData()[0]
        println("**************** descString = $typeData")
        assertEquals(
            typeData,
            Map::class
                .java
                .typeData(
                    String::class.java.typeData(),
                    List::class.java.typeData(Example::class.java.typeData())
                )
        )
    }

    private fun descriptor(type: Class<*>, vararg typeParameters: String): String {
        var desc = Type.getDescriptor(type)
        if (typeParameters.isNotEmpty()) {
            desc =
                desc.dropLast(1) +
                    typeParameters.joinToString("", prefix = "<", postfix = ">") +
                    ";"
        }

        return desc
    }

    private fun Class<*>.typeData(vararg typeParameters: TypeData<*>) =
        TypeData(this, listOf(*typeParameters))

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
        val results =
            type.declaredMethods
                .filter {
                    Modifier.isPublic(it.modifiers) &&
                        !Modifier.isFinal(it.modifiers) &&
                        it.parameterCount == 0
                }
                .sortedBy { it.name }
                .map { method ->
                    try {
                        nameModel.getDeclaredMethod(method.name, *method.parameterTypes)
                        null
                    } catch (e: Exception) {
                        e.message
                    }
                }
                .filterNotNull()

        if (results.isNotEmpty()) {
            fail("Missing methods from ${type.name}: \n${results.joinToString("\n")}")
        }
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
