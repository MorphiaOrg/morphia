package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.getterToPropertyName
import dev.morphia.critter.titleCase
import io.quarkus.gizmo.MethodDescriptor.ofConstructor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import io.quarkus.gizmo.ResultHandle
import io.quarkus.gizmo.SignatureBuilder.*
import io.quarkus.gizmo.Type.*
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Type.getReturnType
import org.objectweb.asm.Type.getType
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class PropertyAccessorGenerator : BaseGizmoGenerator {
    constructor(
        entity: Class<*>,
        critterClassLoader: dev.morphia.critter.CritterClassLoader,
        field: FieldNode,
    ) : super(entity, critterClassLoader) {
        propertyName = field.name
        propertyType = getType(field.desc).className
        generatedType = "${baseName}.${propertyName.titleCase()}Accessor"
    }

    constructor(
        entity: Class<*>,
        critterClassLoader: dev.morphia.critter.CritterClassLoader,
        method: MethodNode,
    ) : super(entity, critterClassLoader) {
        propertyName = method.getterToPropertyName(entity)
        propertyType = getReturnType(method.desc).className
        generatedType = "${baseName}.${propertyName.titleCase()}Accessor"
    }

    val propertyName: String
    val propertyType: String
    val isPrimitive: Boolean
        get() = primitiveToWrapper.containsKey(propertyType)

    val wrapperType: String
        get() = primitiveToWrapper[propertyType] ?: propertyType

    companion object {
        private val primitiveToWrapper =
            mapOf(
                "boolean" to "java.lang.Boolean",
                "byte" to "java.lang.Byte",
                "char" to "java.lang.Character",
                "short" to "java.lang.Short",
                "int" to "java.lang.Integer",
                "long" to "java.lang.Long",
                "float" to "java.lang.Float",
                "double" to "java.lang.Double",
            )
    }

    fun emit(): PropertyAccessorGenerator {
        builder.signature(
            forClass()
                .addInterface(
                    parameterizedType(
                        classType(PropertyAccessor::class.java),
                        classType(propertyType),
                    )
                )
        )

        creator.use {
            ctor()
            get()
            set()
        }

        return this
    }

    private fun get() {
        val method =
            creator.getMethodCreator(
                ofMethod(generatedType, "get", Object::class.java.name, Object::class.java.name)
            )
        method.signature =
            forMethod()
                .addTypeParameter(typeVariable("S"))
                .setReturnType(classType(propertyType))
                .addParameterType(typeVariable("S"))
                .build()
        method.setParameterNames(arrayOf("model"))
        val castModel = method.checkCast(method.getMethodParam(0), entity)
        val toInvoke = ofMethod(entity, "__read${propertyName.titleCase()}", propertyType)
        val result = method.invokeVirtualMethod(toInvoke, castModel)
        val boxed: ResultHandle = if (isPrimitive) method.smartCast(result, wrapperType) else result
        method.returnValue(boxed)
    }

    private fun set() {
        val method =
            creator.getMethodCreator(
                ofMethod(
                    generatedType,
                    "set",
                    "void",
                    Object::class.java.name,
                    Object::class.java.name,
                )
            )
        method.signature =
            forMethod()
                .addTypeParameter(typeVariable("S"))
                .setReturnType(voidType())
                .addParameterType(typeVariable("S"))
                .addParameterType(classType(propertyType))
                .build()
        method.setParameterNames(arrayOf("model", "value"))
        val castModel = method.checkCast(method.getMethodParam(0), entity)
        val castValue: ResultHandle =
            if (isPrimitive) {
                val boxed = method.checkCast(method.getMethodParam(1), wrapperType)
                method.smartCast(boxed, propertyType)
            } else method.checkCast(method.getMethodParam(1), propertyType)
        val toInvoke = ofMethod(entity, "__write${propertyName.titleCase()}", "void", propertyType)
        method.invokeVirtualMethod(toInvoke, castModel, castValue)
        method.returnValue(null)
    }

    private fun ctor() {
        val constructor = creator.getConstructorCreator(*arrayOf<String>())
        constructor.invokeSpecialMethod(ofConstructor(Object::class.java), constructor.`this`)
        constructor.returnVoid()

        constructor.close()
    }
}
