package dev.morphia.critter.parser.gizmo

import com.mongodb.DBRef
import dev.morphia.annotations.AlsoLoad
import dev.morphia.annotations.Reference
import dev.morphia.annotations.internal.AnnotationNodeExtensions.toMorphiaAnnotation
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.conventions.PropertyConvention
import dev.morphia.critter.parser.java.CritterParser.critterClassLoader
import dev.morphia.critter.parser.ksp.extensions.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import io.quarkus.gizmo.ClassCreator
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import io.quarkus.gizmo.ResultHandle
import ksp.com.intellij.codeWithMe.ClientId.Companion.current
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.Type.ARRAY
import org.objectweb.asm.Type.getReturnType
import org.objectweb.asm.Type.getType
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class GizmoPropertyModelGenerator private constructor(val config: MorphiaConfig, entity: Class<*>) :
    BaseGizmoGenerator(entity) {
    constructor(config: MorphiaConfig, entity: Class<*>, field: FieldNode) : this(config, entity) {
        this.field = field
        propertyName = field.name.methodCase()
        propertyType = getType(field.desc)
        val signature = field.signature
        typeArguments =
            signature?.let {
                it.typeData()
                Type.getArgumentTypes("()$it")
            } ?: arrayOf()
        generatedType = "${baseName}.${propertyName.titleCase()}Model"
        accessorType = "${baseName}.${propertyName.titleCase()}Accessor"
        annotations = field.visibleAnnotations ?: emptyList()
    }

    constructor(
        config: MorphiaConfig,
        entity: Class<*>,
        method: MethodNode,
    ) : this(config, entity) {
        this.method = method
        propertyName = method.name.methodCase()
        propertyType = getReturnType(method.desc)
        typeArguments = Type.getArgumentTypes(method.signature)
        generatedType = "${baseName}.${propertyName.titleCase()}Model"
        accessorType = "${baseName}.${propertyName.titleCase()}Accessor"
        annotations = method.visibleAnnotations
    }

    private var typeArguments: Array<Type> = arrayOf()
    var field: FieldNode? = null
    var method: MethodNode? = null
    lateinit var propertyName: String
    lateinit var propertyType: Type
    lateinit var accessorType: String
    lateinit var creator: ClassCreator
    lateinit var annotations: List<AnnotationNode>
    val annotationMap: Map<String, Annotation> by lazy {
        annotations
            .map { it.toMorphiaAnnotation() as Annotation }
            .associateBy { it.annotationClass.qualifiedName!! }
    }
    val accessValue by lazy { field?.access ?: method!!.access }

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
        getFullName()
        getLoadNames()
        getMappedName()
        getName()
        getNormalizedType()
        isArray()
        isFinal()
        isReference()
        isTransient()
        isMap()
        //        isSet()
        creator.close()
    }

    private fun isArray() {
        creator.getMethodCreator("isArray", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.load(propertyType.sort == ARRAY))
        }
    }

    private fun isMap() {
        val type = extractType(propertyType)

        creator.getMethodCreator("isMap", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.load("wait"))
        }
    }

    private fun extractType(type: Type): Any {
        return when {
            type.sort == ARRAY -> extractType(type.elementType)
            //            type.argumentCount != 0 -> extractType(type.argumentTypes.last())
            else -> type
        }
    }

    private fun isFinal() {
        creator.getMethodCreator("isFinal", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.load(checkMask(Opcodes.ACC_FINAL)))
        }
    }

    private fun isTransient() {
        creator.getMethodCreator("isTransient", Boolean::class.java).use { methodCreator ->
            val transient =
                checkMask(Opcodes.ACC_TRANSIENT) or
                    PropertyConvention.transientAnnotations().any {
                        annotationMap.containsKey(it.name)
                    }
            methodCreator.returnValue(methodCreator.load(transient))
        }
    }

    private fun isReference() {
        creator.getMethodCreator("isReference", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(
                methodCreator.load(
                    propertyType.equals(getType(DBRef::class.java)) or
                        annotationMap.containsKey(Reference::class.java.name)
                )
            )
        }
    }

    private fun checkMask(mask: Int) = (accessValue and mask) == mask

    private fun getNormalizedType() {}

    private fun getLoadNames() {
        creator.getMethodCreator("getLoadNames", Array<String>::class.java).use { methodCreator ->
            val alsoLoad: AlsoLoad? = annotationMap[AlsoLoad::class.java.name] as AlsoLoad?
            val size = alsoLoad?.value?.size ?: 0
            val names = methodCreator.newArray(String::class.java, size)
            alsoLoad?.value?.forEachIndexed { index, it ->
                methodCreator.writeArrayValue(names, index, methodCreator.load(it))
            }
            methodCreator.returnValue(names)
        }
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

    private fun getFullName() {
        creator.getMethodCreator("getFullName", String::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.load("${entity.name}#${propertyName}"))
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
        annotation: AnnotationNode,
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

fun String.typeData(): List<TypeData<*>> {
    val bracket = indexOf('<')
    var typeArguments = listOf<TypeData<*>>()

    var current = this
    if (bracket != -1) {
        val substring = substring(indexOf("<") + 1, lastIndexOf('>'))
        typeArguments += substring.typeData()
        current = current.replace("<$substring>", "")
    }

    var // types = mutableListOf<TypeData<*>>()
    types =
        current
            .split(';')
            .filterNot { it.isEmpty() }
            .map { "$it;" }
            .map { TypeData(getType(it).asClass()) }

    /*
        val substring = substring(bracket + 1, lastIndexOf('>'))
        if(substring.indexOf('<') == -1) {
            typeArguments = substring.split(';')
                .filterNot { it.isEmpty() }
                .map { "$it;" }
                .map { it.typeData() }
        } else {
            TODO()
        }
        current = current.removeRange(bracket, lastIndexOf('>') + 1)
    */
    if (types.size == 1) {
        types = listOf(TypeData(types[0].type, typeArguments))
    }
    return types
}

private fun Type.asClass(): Class<*> {
    return Class.forName(className)
}
