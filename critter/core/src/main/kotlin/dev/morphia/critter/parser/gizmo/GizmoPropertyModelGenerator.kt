package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.parser.ksp.extensions.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import io.quarkus.gizmo.ClassCreator
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.ResultHandle
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Type
import org.objectweb.asm.Type.getType
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode

class GizmoPropertyModelGenerator : BaseGizmoGenerator {

    constructor(entity: Class<*>, field: FieldNode) : super(entity) {
        propertyName = field.name.titleCase()
        generatedType = "${baseName}.${propertyName}Model"
        accessorType = "${baseName}.${propertyName}Accessor"
        annotations = field.visibleAnnotations ?: listOf()
    }

    val propertyName: String
    val accessorType: String
    lateinit var creator: ClassCreator
    lateinit var annotations: List<AnnotationNode>

    fun emit() {
        creator =
            ClassCreator.builder()
                .classOutput { name, data ->
                    critterClassLoader.register(name.replace('/', '.'), data)
                }
                .className(generatedType)
                .superClass(CritterPropertyModel::class.java)
                .build()

        ctor()
        getAccessor()

        creator.close()
    }

    private fun ctor() {
        val constructor = creator.getConstructorCreator(EntityModel::class.java)
        constructor.invokeSpecialMethod(
            MethodDescriptor.ofConstructor(PropertyModel::class.java, EntityModel::class.java),
            constructor.`this`,
            constructor.getMethodParam(0)
        )
        constructor.setParameterNames(arrayOf("model"))

        registerAnnotations(constructor)

        constructor.close()
    }

    private fun registerAnnotations(constructor: MethodCreator) {
        val annotationMethod =
            MethodDescriptor.ofMethod(
                PropertyModel::class.java.name,
                "annotation",
                PropertyModel::class.java.name,
                Annotation::class.java
            )
        annotations.forEach { annotation ->
            constructor.invokeVirtualMethod(
                annotationMethod,
                constructor.`this`,
                annotationBuilder(constructor, annotation)
            )
        }
    }

    private fun annotationBuilder(
        constructor: MethodCreator,
        annotation: AnnotationNode
    ): ResultHandle {
        val type = getType(annotation.desc)
        val classType = type.className.substringAfterLast('.')
        val builderType = Type.getType("L${type.className}Builder;")
        val builder =
            MethodDescriptor.ofMethod(
                builderType.className,
                "${classType.methodCase()}Builder",
                builderType.className
            )

        var local = constructor.invokeStaticMethod(builder)
        val values = annotation.values?.windowed(2, 2) ?: emptyList()
        values.forEach { value ->
            val method =
                MethodDescriptor.ofMethod(
                    builderType.className,
                    value[0] as String,
                    builderType.className,
                    value[1].javaClass
                )
            constructor.invokeVirtualMethod(method, local, load(constructor, value[1]))
        }

        return local
    }

    private fun load(constructor: MethodCreator, value: Any): ResultHandle {
        return when (value) {
            is String -> constructor.load(value)
            is Int -> constructor.load(value)
            is Long -> constructor.load(value)
            is Boolean -> constructor.load(value)
            is List<*> -> {
                val toTypedArray = value.map { load(constructor, it!!) }.toTypedArray()
                constructor.marshalAsArray(value[0]!!.javaClass, *toTypedArray)
            }
            else -> TODO("${value.javaClass} is not yet supported")
        }
    }

    private fun getAccessor() {
        val field = creator.getFieldCreator("accessor", accessorType)
        val method =
            creator.getMethodCreator(
                MethodDescriptor.ofMethod(
                    creator.className,
                    "getAccessor",
                    PropertyAccessor::class.java.name
                )
            )

        method.returnValue(method.readInstanceField(field.fieldDescriptor, method.`this`))

        method.close()
    }
}
