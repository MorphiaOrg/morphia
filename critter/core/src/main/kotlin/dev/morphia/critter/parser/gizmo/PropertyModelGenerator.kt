package dev.morphia.critter.parser.gizmo

import com.mongodb.DBRef
import dev.morphia.annotations.AlsoLoad
import dev.morphia.annotations.Reference
import dev.morphia.annotations.internal.AnnotationNodeExtensions.toMorphiaAnnotation
import dev.morphia.config.MorphiaConfig
import dev.morphia.critter.conventions.PropertyConvention
import dev.morphia.critter.parser.Generators.isArray
import dev.morphia.critter.parser.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.PropertyModel.normalize
import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import io.quarkus.gizmo.MethodCreator
import io.quarkus.gizmo.MethodDescriptor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import io.quarkus.gizmo.ResultHandle
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.Type.ARRAY
import org.objectweb.asm.Type.getReturnType
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class PropertyModelGenerator private constructor(val config: MorphiaConfig, entity: Class<*>) :
    BaseGizmoGenerator(entity) {
    constructor(config: MorphiaConfig, entity: Class<*>, field: FieldNode) : this(config, entity) {
        this.field = field
        propertyName = field.name.methodCase()
        propertyType = Type.getType(field.desc)
        val signature = field.signature
        typeArguments =
            signature?.let {
                typeData(it)
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
    lateinit var annotations: List<AnnotationNode>
    val annotationMap: Map<String, Annotation> by lazy {
        annotations
            .map { it.toMorphiaAnnotation() as Annotation }
            .associateBy { it.annotationClass.qualifiedName!! }
    }
    val accessValue by lazy { field?.access ?: method!!.access }
    val typeData by lazy {
        val input =
            field?.let<FieldNode, String> { it.signature ?: it.desc }
                ?: method!!.let<MethodNode, String> { it.signature ?: it.desc }
        typeData(input)[0]
    }
    val model by lazy { creator.getFieldCreator("entityModel", EntityModel::class.java) }

    fun emit(): PropertyModelGenerator {
        builder.superClass(CritterPropertyModel::class.java)

        creator.use {
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
            isSet()
            isCollection()
            getType()
            getTypeData()
            getEntityModel()
        }
        return this
    }

    private fun isArray() {
        creator.getMethodCreator("isArray", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.load(propertyType.isArray()))
        }
    }

    private fun isMap() {
        creator.getMethodCreator("isMap", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(
                methodCreator.load(Map::class.java.isAssignableFrom(typeData.type))
            )
        }
    }

    private fun isSet() {
        creator.getMethodCreator("isSet", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(
                methodCreator.load(Set::class.java.isAssignableFrom(typeData.type))
            )
        }
    }

    private fun getType() {
        creator.getMethodCreator("getType", Class::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.loadClass(typeData.type))
        }
    }

    private fun getEntityModel() {
        creator.getMethodCreator("getEntityModel", EntityModel::class.java).use { methodCreator ->
            methodCreator.returnValue(
                methodCreator.readInstanceField(model.fieldDescriptor, methodCreator.`this`)
            )
        }
    }

    private fun getTypeData() {
        creator.getMethodCreator("getTypeData", TypeData::class.java).use { methodCreator ->
            methodCreator.returnValue(emitTypeData(methodCreator, this.typeData))
        }
    }

    private fun emitTypeData(methodCreator: MethodCreator, data: TypeData<*>): ResultHandle {
        var array = methodCreator.newArray(TypeData::class.java, data.typeParameters.size)

        data.typeParameters.forEachIndexed { index, typeParameter ->
            methodCreator.writeArrayValue(array, index, emitTypeData(methodCreator, typeParameter))
        }
        val list = listOf(methodCreator.loadClass(data.type), array)
        val descriptor =
            MethodDescriptor.ofConstructor(
                TypeData::class.java,
                Class::class.java,
                "[${Type.getType(TypeData::class.java).descriptor}"
            )
        return methodCreator.newInstance(descriptor, *list.toTypedArray())
    }

    private fun isCollection() {
        creator.getMethodCreator("isCollection", Boolean::class.java).use { methodCreator ->
            methodCreator.returnValue(
                methodCreator.load(Collection::class.java.isAssignableFrom(typeData.type))
            )
        }
    }

    private fun extractType(type: Type): Any {
        return when {
            type.sort == ARRAY -> extractType(type.elementType)
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
                    propertyType.equals(Type.getType(DBRef::class.java)) or
                        annotationMap.containsKey(Reference::class.java.name)
                )
            )
        }
    }

    private fun checkMask(mask: Int) = (accessValue and mask) == mask

    private fun getNormalizedType() {
        creator.getMethodCreator("getNormalizedType", Class::class.java).use { methodCreator ->
            methodCreator.returnValue(methodCreator.loadClass(normalize(typeData)))
        }
    }

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
            constructor.writeInstanceField(
                model.fieldDescriptor,
                constructor.`this`,
                constructor.getMethodParam(0)
            )
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
                annotation.annotationBuilder(constructor)
            )
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

private fun String.balanced(): String {
    if (!contains("<")) return ""
    val start = indexOf('<') + 1
    var index = start
    var count = 1

    while (index < length && count != 0) {
        when (this[++index]) {
            '>' -> count--
            '<' -> count++
        }
    }

    return substring(start, index)
}

fun typeData(input: String): List<TypeData<*>> {
    if (input.isEmpty()) return emptyList()
    val types = mutableListOf<TypeData<*>>()
    var value = input

    while (value.isNotEmpty()) {
        val bracket = value.indexOf('<')
        if (bracket == -1 || bracket > value.indexOf(';')) {
            var type = value.substringBefore(';')
            if (type.length > 2) {
                type += ";"
            }
            value = value.substring(type.length)
            val type1 = Type.getType(type)
            types += type1.typeData()
        } else {
            val paramString = value.balanced()
            value = value.replace("<$paramString>", "")
            types += Type.getType(value).typeData(typeData(paramString))
            value = value.substringAfter(';')
        }
    }
    return types
}
