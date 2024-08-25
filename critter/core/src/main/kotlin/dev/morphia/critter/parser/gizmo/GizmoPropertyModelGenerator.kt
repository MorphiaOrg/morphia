package dev.morphia.critter.parser.gizmo

import dev.morphia.annotations.internal.AnnotationNodeExtensions.toMorphiaAnnotation
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.conventions.PropertyConvention
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.parser.ksp.extensions.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import io.quarkus.gizmo.ClassCreator
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import io.quarkus.gizmo.ResultHandle
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Type.getType
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class GizmoPropertyModelGenerator private constructor(val config: MorphiaConfig, entity: Class<*>) :
    BaseGizmoGenerator(entity) {

    constructor(config: MorphiaConfig, entity: Class<*>, field: FieldNode) : this(config, entity) {
        propertyName = field.name.methodCase()
        generatedType = "${baseName}.${propertyName.titleCase()}Model"
        accessorType = "${baseName}.${propertyName.titleCase()}Accessor"
        annotations = field.visibleAnnotations
    }

    constructor(
        config: MorphiaConfig,
        entity: Class<*>,
        method: MethodNode
    ) : this(config, entity) {
        propertyName = method.name.methodCase()
        generatedType = "${baseName}.${propertyName.titleCase()}Model"
        accessorType = "${baseName}.${propertyName.titleCase()}Accessor"
        annotations = method.visibleAnnotations
    }

    lateinit var propertyName: String
    lateinit var accessorType: String
    lateinit var creator: ClassCreator
    lateinit var annotations: List<AnnotationNode>
    val annotationMap: Map<String, Annotation> by lazy {
        annotations
            .map { it.toMorphiaAnnotation() as Annotation }
            .associateBy { it.annotationClass.qualifiedName!! }
    }

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
        getName()
        getMappedName()

        creator.close()
    }

    private fun getName() {
        creator.getMethodCreator("getName", String::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.load(propertyName))
        }
    }

    private fun getMappedName() {
        creator.getMethodCreator("getMappedName", String::class.java).use { methodCreator ->
            methodCreator.returnValue(
                methodCreator.load(
                    PropertyConvention.mappedName(config, annotationMap, propertyName)
                )
            )
        }
    }

    private fun ctor() {
        creator.getConstructorCreator(EntityModel::class.java).use { constructor ->
            constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(
                    CritterPropertyModel::class.java,
                    EntityModel::class.java
                ),
                constructor.getThis(),
                constructor.getMethodParam(0)
            )
            constructor.setParameterNames(arrayOf("model"))
            registerAnnotations(constructor)
            constructor.returnVoid()
        }
    }

    private fun registerAnnotations(constructor: MethodCreator) {
        val annotationMethod =
            ofMethod(
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
        val classPackage = type.className.substringBeforeLast('.')
        val className = type.className.substringAfterLast('.')
        val builderType =
            getType("L${classPackage}.internal.${className}Builder;".replace('.', '/'))
        val builder =
            ofMethod(
                builderType.className,
                "${className.methodCase()}Builder",
                builderType.className
            )

        val local = constructor.invokeStaticMethod(builder)
        val values = annotation.values?.windowed(2, 2) ?: emptyList()
        values.forEach { value ->
            val method =
                ofMethod(
                    builderType.className,
                    value[0] as String,
                    builderType.className,
                    when (value[1]) {
                        is List<*> -> Array<String>::class.java
                        else -> value[1].javaClass
                    }
                )
            constructor.invokeVirtualMethod(method, local, load(constructor, value[1]))
        }

        return constructor.invokeVirtualMethod(
            ofMethod(builderType.className, "build", type.className),
            local
        )
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
                ofMethod(creator.className, "getAccessor", PropertyAccessor::class.java.name)
            )

        method.returnValue(method.readInstanceField(field.fieldDescriptor, method.`this`))

        method.close()
    }
}
