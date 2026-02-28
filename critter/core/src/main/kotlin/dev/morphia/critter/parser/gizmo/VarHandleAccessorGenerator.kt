package dev.morphia.critter.parser.gizmo

import dev.morphia.critter.parser.getterToPropertyName
import dev.morphia.critter.titleCase
import io.quarkus.gizmo.FieldDescriptor
import io.quarkus.gizmo.MethodDescriptor.ofConstructor
import io.quarkus.gizmo.MethodDescriptor.ofMethod
import io.quarkus.gizmo.SignatureBuilder.forClass
import io.quarkus.gizmo.SignatureBuilder.forMethod
import io.quarkus.gizmo.Type.classType
import io.quarkus.gizmo.Type.parameterizedType
import io.quarkus.gizmo.Type.typeVariable
import io.quarkus.gizmo.Type.voidType
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodHandles.Lookup
import java.lang.invoke.MethodType
import java.lang.invoke.VarHandle
import java.lang.reflect.Modifier
import org.bson.codecs.pojo.PropertyAccessor
import org.objectweb.asm.Type.getReturnType
import org.objectweb.asm.Type.getType
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class VarHandleAccessorGenerator : BaseGizmoGenerator {
    constructor(
        entity: Class<*>,
        critterClassLoader: dev.morphia.critter.CritterClassLoader,
        field: FieldNode,
    ) : super(entity, critterClassLoader) {
        propertyName = field.name
        propertyType = getType(field.desc).className
        isFieldBased = true
        getterName = null
        setterName = null
        generatedType = "${baseName}.${propertyName.titleCase()}Accessor"
    }

    constructor(
        entity: Class<*>,
        critterClassLoader: dev.morphia.critter.CritterClassLoader,
        method: MethodNode,
    ) : super(entity, critterClassLoader) {
        propertyName = method.getterToPropertyName(entity)
        propertyType = getReturnType(method.desc).className
        isFieldBased = false
        getterName = method.name
        setterName = "set${propertyName.titleCase()}"
        generatedType = "${baseName}.${propertyName.titleCase()}Accessor"
    }

    val propertyName: String
    val propertyType: String
    val isFieldBased: Boolean
    val getterName: String?
    val setterName: String?

    val isPrimitive: Boolean
        get() = primitiveToWrapper.containsKey(propertyType)

    val wrapperType: String
        get() = primitiveToWrapper[propertyType] ?: propertyType

    private val hasSetter: Boolean by lazy {
        if (isFieldBased || setterName == null) return@lazy false
        val paramClass =
            if (isPrimitive) primitiveClass(propertyType) else Class.forName(propertyType)
        try {
            entity.getDeclaredMethod(setterName!!, paramClass)
            true
        } catch (_: NoSuchMethodException) {
            false
        }
    }

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

        private val primitiveClasses =
            mapOf(
                "boolean" to Boolean::class.javaPrimitiveType!!,
                "byte" to Byte::class.javaPrimitiveType!!,
                "char" to Char::class.javaPrimitiveType!!,
                "short" to Short::class.javaPrimitiveType!!,
                "int" to Int::class.javaPrimitiveType!!,
                "long" to Long::class.javaPrimitiveType!!,
                "float" to Float::class.javaPrimitiveType!!,
                "double" to Double::class.javaPrimitiveType!!,
            )

        private fun primitiveClass(name: String): Class<*> =
            primitiveClasses[name] ?: error("Not a primitive: $name")
    }

    fun emit(): VarHandleAccessorGenerator {
        builder.signature(
            forClass()
                .addInterface(
                    parameterizedType(
                        classType(PropertyAccessor::class.java),
                        classType(propertyType),
                    )
                )
        )

        // Declare handle field(s) on the class
        val handleDesc: FieldDescriptor
        var setterHandleDesc: FieldDescriptor? = null

        if (isFieldBased) {
            handleDesc =
                creator
                    .getFieldCreator("varHandle", VarHandle::class.java)
                    .also { it.setModifiers(Modifier.PRIVATE or Modifier.FINAL) }
                    .fieldDescriptor
        } else {
            handleDesc =
                creator
                    .getFieldCreator("getterHandle", MethodHandle::class.java)
                    .also { it.setModifiers(Modifier.PRIVATE or Modifier.FINAL) }
                    .fieldDescriptor
            if (hasSetter) {
                setterHandleDesc =
                    creator
                        .getFieldCreator("setterHandle", MethodHandle::class.java)
                        .also { it.setModifiers(Modifier.PRIVATE or Modifier.FINAL) }
                        .fieldDescriptor
            }
        }

        creator.use {
            ctor(handleDesc, setterHandleDesc)
            get(handleDesc)
            set(handleDesc, setterHandleDesc)
        }

        return this
    }

    private fun ctor(handleDesc: FieldDescriptor, setterHandleDesc: FieldDescriptor?) {
        val constructor = creator.getConstructorCreator(*arrayOf<String>())
        constructor.invokeSpecialMethod(ofConstructor(Object::class.java), constructor.`this`)

        val tryBlock = constructor.tryBlock()

        val callerLookup =
            tryBlock.invokeStaticMethod(
                ofMethod(MethodHandles::class.java, "lookup", Lookup::class.java)
            )

        // Load entity class via TCCL to avoid classloader mismatch: CritterClassLoader
        // auto-registers dev.morphia.critter.* classes creating a second incompatible version.
        val currentThread =
            tryBlock.invokeStaticMethod(
                ofMethod(Thread::class.java, "currentThread", Thread::class.java)
            )
        val tccl =
            tryBlock.invokeVirtualMethod(
                ofMethod(Thread::class.java, "getContextClassLoader", ClassLoader::class.java),
                currentThread,
            )
        val entityClass =
            tryBlock.invokeStaticMethod(
                ofMethod(
                    Class::class.java,
                    "forName",
                    Class::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    ClassLoader::class.java,
                ),
                tryBlock.load(entity.name),
                tryBlock.load(true),
                tccl,
            )
        val privateLookup =
            tryBlock.invokeStaticMethod(
                ofMethod(
                    MethodHandles::class.java,
                    "privateLookupIn",
                    Lookup::class.java,
                    Class::class.java,
                    Lookup::class.java,
                ),
                entityClass,
                callerLookup,
            )

        if (isFieldBased) {
            val fieldTypeClass = tryBlock.loadClass(propertyType)
            val handle =
                tryBlock.invokeVirtualMethod(
                    ofMethod(
                        Lookup::class.java,
                        "findVarHandle",
                        VarHandle::class.java,
                        Class::class.java,
                        String::class.java,
                        Class::class.java,
                    ),
                    privateLookup,
                    entityClass,
                    tryBlock.load(propertyName),
                    fieldTypeClass,
                )
            tryBlock.writeInstanceField(handleDesc, tryBlock.`this`, handle)
        } else {
            // Getter MethodHandle
            val returnTypeClass = tryBlock.loadClass(propertyType)
            val getterMethodType =
                tryBlock.invokeStaticMethod(
                    ofMethod(
                        MethodType::class.java,
                        "methodType",
                        MethodType::class.java,
                        Class::class.java,
                    ),
                    returnTypeClass,
                )
            val getterHandle =
                tryBlock.invokeVirtualMethod(
                    ofMethod(
                        Lookup::class.java,
                        "findVirtual",
                        MethodHandle::class.java,
                        Class::class.java,
                        String::class.java,
                        MethodType::class.java,
                    ),
                    privateLookup,
                    entityClass,
                    tryBlock.load(getterName!!),
                    getterMethodType,
                )
            tryBlock.writeInstanceField(handleDesc, tryBlock.`this`, getterHandle)

            // Setter MethodHandle (if applicable)
            if (setterHandleDesc != null) {
                val voidClass = tryBlock.loadClass(Void.TYPE)
                val paramTypeClass = tryBlock.loadClass(propertyType)
                val setterMethodType =
                    tryBlock.invokeStaticMethod(
                        ofMethod(
                            MethodType::class.java,
                            "methodType",
                            MethodType::class.java,
                            Class::class.java,
                            Class::class.java,
                        ),
                        voidClass,
                        paramTypeClass,
                    )
                val setterHandle =
                    tryBlock.invokeVirtualMethod(
                        ofMethod(
                            Lookup::class.java,
                            "findVirtual",
                            MethodHandle::class.java,
                            Class::class.java,
                            String::class.java,
                            MethodType::class.java,
                        ),
                        privateLookup,
                        entityClass,
                        tryBlock.load(setterName!!),
                        setterMethodType,
                    )
                tryBlock.writeInstanceField(setterHandleDesc, tryBlock.`this`, setterHandle)
            }
        }

        val catchBlock = tryBlock.addCatch(ReflectiveOperationException::class.java)
        val ex = catchBlock.getCaughtException()
        val wrapped =
            catchBlock.newInstance(
                ofConstructor(RuntimeException::class.java, Throwable::class.java),
                ex,
            )
        catchBlock.throwException(wrapped)

        constructor.returnVoid()
        constructor.close()
    }

    private fun get(handleDesc: FieldDescriptor) {
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

        // Do NOT checkCast to entity type — VarHandle/MethodHandle performs its own type check
        // and casting would cause classloader mismatch (CritterClassLoader vs app classloader).
        val model = method.getMethodParam(0)
        val handleRef = method.readInstanceField(handleDesc, method.`this`)

        val result =
            if (isFieldBased) {
                method.invokeVirtualMethod(
                    ofMethod(
                        VarHandle::class.java,
                        "get",
                        Object::class.java.name,
                        Object::class.java.name,
                    ),
                    handleRef,
                    model,
                )
            } else {
                method.invokeVirtualMethod(
                    ofMethod(
                        MethodHandle::class.java,
                        "invoke",
                        Object::class.java.name,
                        Object::class.java.name,
                    ),
                    handleRef,
                    model,
                )
            }

        val boxed = if (isPrimitive) method.smartCast(result, wrapperType) else result
        method.returnValue(boxed)
    }

    private fun set(handleDesc: FieldDescriptor, setterHandleDesc: FieldDescriptor?) {
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

        if (!isFieldBased && setterHandleDesc == null) {
            // Read-only property — no setter
            method.throwException(
                UnsupportedOperationException::class.java,
                "Property '$propertyName' is read-only",
            )
            return
        }

        // Do NOT checkCast to entity type — same classloader mismatch concern as in get().
        val castModel = method.getMethodParam(0)
        val castValue =
            if (isPrimitive) {
                val boxed = method.checkCast(method.getMethodParam(1), wrapperType)
                method.smartCast(boxed, propertyType)
            } else {
                method.checkCast(method.getMethodParam(1), propertyType)
            }

        val handleRef =
            if (isFieldBased) method.readInstanceField(handleDesc, method.`this`)
            else method.readInstanceField(setterHandleDesc!!, method.`this`)

        if (isFieldBased) {
            method.invokeVirtualMethod(
                ofMethod(
                    VarHandle::class.java,
                    "set",
                    "void",
                    Object::class.java.name,
                    Object::class.java.name,
                ),
                handleRef,
                castModel,
                castValue,
            )
        } else {
            method.invokeVirtualMethod(
                ofMethod(
                    MethodHandle::class.java,
                    "invoke",
                    "void",
                    Object::class.java.name,
                    Object::class.java.name,
                ),
                handleRef,
                castModel,
                castValue,
            )
        }

        method.returnValue(null)
    }
}
