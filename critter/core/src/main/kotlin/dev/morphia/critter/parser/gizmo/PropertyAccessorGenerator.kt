package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.titleCase
import io.quarkus.gizmo.MethodDescriptor.ofConstructor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import io.quarkus.gizmo.SignatureBuilder.*
import io.quarkus.gizmo.Type.*
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Type.getReturnType
import org.objectweb.asm.Type.getType
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class PropertyAccessorGenerator : BaseGizmoGenerator {
    constructor(entity: Class<*>, field: FieldNode) : super(entity) {
        propertyName = field.name
        propertyType = getType(field.desc).className
        generatedType = "${baseName}.${propertyName.titleCase()}Accessor"
    }

    constructor(entity: Class<*>, method: MethodNode) : super(entity) {
        propertyName = method.name
        propertyType = getReturnType(method.signature).className
        generatedType = "${baseName}.${propertyName.titleCase()}Accessor"
    }

    val propertyName: String
    val propertyType: String

    fun emit(): PropertyAccessorGenerator {
        builder.signature(
            forClass()
                .addInterface(
                    parameterizedType(
                        classType(PropertyAccessor::class.java),
                        classType(propertyType)
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
            creator.getMethodCreator(ofMethod(generatedType, "get", propertyType, entity.name))
        method.signature =
            forMethod()
                .addTypeParameter(typeVariable("S"))
                .setReturnType(classType(propertyType))
                .addParameterType(classType(entity))
                .build()
        method.setParameterNames(arrayOf("model"))
        val toInvoke = ofMethod(entity, "__read${propertyName.titleCase()}", propertyType)
        method.returnValue(method.invokeVirtualMethod(toInvoke, method.getMethodParam(0)))
    }

    private fun set() {
        val method =
            creator.getMethodCreator(
                ofMethod(generatedType, "set", "void", entity.name, propertyType)
            )
        method.signature =
            forMethod()
                .addTypeParameter(typeVariable("S"))
                .setReturnType(voidType())
                .addParameterType(classType(entity))
                .addParameterType(classType(propertyType))
                .build()
        method.setParameterNames(arrayOf("model", "value"))
        val toInvoke = ofMethod(entity, "__write${propertyName.titleCase()}", "void", propertyType)
        method.invokeVirtualMethod(toInvoke, method.getMethodParam(0), method.getMethodParam(1))
        method.returnValue(null)
    }

    private fun ctor() {
        val constructor = creator.getConstructorCreator(*arrayOf<String>())
        constructor.invokeSpecialMethod(ofConstructor(Object::class.java), constructor.`this`)
        constructor.returnVoid()

        constructor.close()
    }
}
