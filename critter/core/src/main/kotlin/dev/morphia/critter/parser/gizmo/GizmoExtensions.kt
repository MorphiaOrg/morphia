package dev.morphia.critter.parser.gizmo

import dev.morphia.annotations.internal.AnnotationNodeExtensions.setBuilderValues
import dev.morphia.critter.parser.Generators.asClass
import dev.morphia.critter.parser.methodCase
import dev.morphia.mapping.codec.pojo.TypeData
import io.quarkus.gizmo.FieldDescriptor
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.ResultHandle
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
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

fun TypeData<*>.emitTypeData(methodCreator: MethodCreator): ResultHandle {
    var array = methodCreator.newArray(TypeData::class.java, typeParameters.size)

    typeParameters.forEachIndexed { index, typeParameter ->
        methodCreator.writeArrayValue(array, index, typeParameter.emitTypeData(methodCreator))
    }
    val list = listOf(methodCreator.loadClass(type), array)
    val descriptor =
        MethodDescriptor.ofConstructor(
            TypeData::class.java,
            Class::class.java,
            "[${Type.getType(TypeData::class.java).descriptor}"
        )
    return methodCreator.newInstance(descriptor, *list.toTypedArray())
}

fun rawType(type: java.lang.reflect.Type) =
    when (type) {
        is GenericArrayType -> {
            val type1 = type.genericComponentType as ParameterizedType
            Type.getType("[" + Type.getType(type1.rawType as Class<*>).descriptor)
        }
        else -> Type.getType(type as Class<*>)
    }.descriptor

fun attributeType(type: KClass<*>, name: String): java.lang.reflect.Type {
    val map =
        type.declaredMemberProperties
            .filter { it.name == name }
            .map { it.returnType.javaType }
            .first()
    return map
}

@Suppress("UNCHECKED_CAST")
fun load(creator: MethodCreator, type: java.lang.reflect.Type, `value`: Any): ResultHandle {
    fun extractComponentType(arrayType: GenericArrayType) =
        (arrayType.genericComponentType as ParameterizedType).rawType.typeName

    return when (type) {
        is Class<*> -> load(creator, type, value)
        is GenericArrayType -> {
            val genericComponentType = extractComponentType(type)
            val newArray = creator.newArray(genericComponentType, (value as List<Any>).size)
            newArray.apply<ResultHandle> {
                value.forEachIndexed { index: Int, element: Any ->
                    creator.writeArrayValue(
                        this,
                        index,
                        load(creator, Class.forName((element as Type).className), element)
                    )
                }
            }
            newArray
        }
        else -> TODO("unknown type: $type")
    }
}

@Suppress("UNCHECKED_CAST")
fun load(creator: MethodCreator, type: Class<*>, `value`: Any): ResultHandle {
    return when (type) {
        String::class.java -> creator.load(value as String)
        Int::class.java -> creator.load(value as Int)
        Long::class.java -> creator.load(value as Long)
        Boolean::class.java -> creator.load(value as Boolean)
        AnnotationNode::class.java -> (value as AnnotationNode).annotationBuilder(creator)
        else -> {
            when {
                type.isAnnotation -> (value as AnnotationNode).annotationBuilder(creator)
                type.isArray ->
                    creator.newArray(type.componentType, (value as List<Any>).size).apply<
                        ResultHandle
                    > {
                        value.forEachIndexed { index: Int, element: Any ->
                            creator.writeArrayValue(
                                this,
                                index,
                                load(creator, element.javaClass, element)
                            )
                        }
                    }
                type.isEnum ->
                    return creator.readStaticField(
                        FieldDescriptor.of(type, (value as Array<String>)[1], type)
                    )
                value is Type -> {
                    creator.loadClass(value.className)
                }
                else -> TODO("$type is not yet supported")
            }
        }
    }
}

fun Type.typeData(typeParameters: List<TypeData<*>> = listOf()) =
    TypeData(asClass(), typeParameters)
