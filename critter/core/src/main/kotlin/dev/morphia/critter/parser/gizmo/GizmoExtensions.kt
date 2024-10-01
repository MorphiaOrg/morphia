package dev.morphia.critter.parser.gizmo

import dev.morphia.annotations.internal.AnnotationNodeExtensions.setBuilderValues
import dev.morphia.critter.parser.methodCase
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.ResultHandle
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode

fun AnnotationNode.annotationBuilder(creator: MethodCreator): ResultHandle {
    val type = Type.getType(desc)
    val classPackage = type.className.substringBeforeLast('.')
    val className = type.className.substringAfterLast('.')
    val builderType =
        Type.getType("L${classPackage}.internal.${className}Builder;".replace('.', '/'))
    val builder =
        MethodDescriptor.ofMethod(
            builderType.className,
            "${className.methodCase()}Builder",
            builderType.className
        )
    val local = creator.invokeStaticMethod(builder)

    setBuilderValues(creator, local)

    return creator.invokeVirtualMethod(
        MethodDescriptor.ofMethod(builderType.className, "build", type.className),
        local
    )
}

fun attributeType(type: KClass<*>, name: String) =
    type.declaredMemberProperties.filter { it.name == name }.map { it.returnType.javaType }.first()
        as Class<*>

@OptIn(ExperimentalStdlibApi::class)
public fun load(creator: MethodCreator, type: Class<*>, `value`: Any): ResultHandle {
    return when (type) {
        String::class.java -> creator.load(value as String)
        Int::class.java -> creator.load(value as Int)
        Long::class.java -> creator.load(value as Long)
        Boolean::class.java -> creator.load(value as Boolean)
        AnnotationNode::class.java -> (value as AnnotationNode).annotationBuilder(creator)
        else -> {
            return if (type.isArray) {
                @Suppress("UNCHECKED_CAST")
                value as List<Any>
                val handle = creator.newArray(type.componentType, value.size)
                value.forEachIndexed { index: Int, element: Any ->
                    creator.writeArrayValue(
                        handle,
                        index,
                        load(creator, element.javaClass, element)
                    )
                }

                return handle
            } else TODO("$type is not yet supported")
        }
    }
}
