package dev.morphia.critter.parser.generators

import dev.morphia.annotations.Entity
import dev.morphia.critter.CritterEntityModel
import dev.morphia.critter.parser.generators.Generators.critterPackage
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode

class CritterEntityModelGenerator(val entity: Class<*>, val models: List<String>) :
    BaseGenerator(entity) {
    companion object {
        private val baseType = Type.getType(CritterEntityModel::class.java)
    }

    init {
        val generatorName = "${critterPackage(entity)}${entity.simpleName}EntityModel"
        generatedType = Type.getType("L${generatorName};")
    }

    override fun emit(): ByteArray {
        classWriter.visit(
            V17,
            accessFlags(),
            generatedType.internalName,
            null,
            baseType.internalName,
            null
        )

        constructor()
        colllectionName()
        discriminator()
        discriminatorKey()
        useDiscriminator()
        getEntityAnnotation()
        getType()
        isAbstract()
        isInterface()

        classWriter.visitEnd()

        return classWriter.toByteArray()
    }

    fun constructor() {
        val mv = method(ACC_PUBLIC, "<init>", "(Ldev/morphia/mapping/Mapper;)V", null, null, 24)
        mv.visitCode()
        val label0 = label(mv)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitVarInsn(ALOAD, 1)
        mv.visitLdcInsn(Type.getType(entityType.descriptor))
        mv.visitMethodInsn(
            INVOKESPECIAL,
            "dev/morphia/critter/CritterEntityModel",
            "<init>",
            "(Ldev/morphia/mapping/Mapper;Ljava/lang/Class;)V",
            false
        )

        propertyModels(mv, models)

        val label1 = label(mv)
        mv.visitInsn(RETURN)
        val label2 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label2, 0)
        mv.visitLocalVariable("mapper", "Ldev/morphia/mapping/Mapper;", null, label0, label2, 1)
        //        mv.visitMaxs(4, 2)
        mv.visitEnd()
    }

    private fun propertyModels(mv: MethodVisitor, models: List<String>) {
        models.forEach {
            val model = it.replace('.', '/')
            var label = label(mv)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitTypeInsn(NEW, model)
            mv.visitInsn(DUP)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(
                INVOKESPECIAL,
                model,
                "<init>",
                "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
                false
            )
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                generatedType.internalName,
                "addProperty",
                "(Ldev/morphia/mapping/codec/pojo/PropertyModel;)Z",
                false
            )
            mv.visitInsn(POP)
        }
    }

    fun colllectionName() {
        val mv = method(ACC_PUBLIC, "collectionName", "()Ljava/lang/String;", null, null, 37)
        mv.visitCode()
        val label0 = label(mv, 37)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(
            GETFIELD,
            generatedType.internalName,
            "mapper",
            "Ldev/morphia/mapping/Mapper;"
        )
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "dev/morphia/mapping/Mapper",
            "getConfig",
            "()Ldev/morphia/config/MorphiaConfig;",
            false
        )
        mv.visitMethodInsn(
            INVOKEINTERFACE,
            "dev/morphia/config/MorphiaConfig",
            "collectionNaming",
            "()Ldev/morphia/mapping/NamingStrategy;",
            true
        )
        mv.visitLdcInsn(entityType.internalName.substringAfterLast("/"))
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "dev/morphia/mapping/NamingStrategy",
            "apply",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
        )
        mv.visitInsn(ARETURN)
        val label1 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        //        mv.visitMaxs(2, 1)
        mv.visitEnd()
    }

    fun discriminator() {
        val mv = method(ACC_PUBLIC, "discriminator", "()Ljava/lang/String;", null, null, 47)
        mv.visitCode()
        val label0 = label(mv)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(
            GETFIELD,
            generatedType.internalName,
            "mapper",
            "Ldev/morphia/mapping/Mapper;"
        )
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "dev/morphia/mapping/Mapper",
            "getConfig",
            "()Ldev/morphia/config/MorphiaConfig;",
            false
        )
        mv.visitMethodInsn(
            INVOKEINTERFACE,
            "dev/morphia/config/MorphiaConfig",
            "discriminator",
            "()Ldev/morphia/mapping/DiscriminatorFunction;",
            true
        )
        mv.visitLdcInsn(entityType)
        mv.visitLdcInsn(entity.getAnnotation(Entity::class.java).discriminator)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "dev/morphia/mapping/DiscriminatorFunction",
            "apply",
            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;",
            false
        )
        mv.visitInsn(ARETURN)
        val label1 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(3, 1)
        mv.visitEnd()
    }

    fun discriminatorKey() {
        val mv = method(ACC_PUBLIC, "discriminatorKey", "()Ljava/lang/String;", null, null, 52)
        mv.visitCode()
        val label0 = label(mv)
        mv.visitLdcInsn(entity.getAnnotation(Entity::class.java).discriminatorKey)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(
            GETFIELD,
            generatedType.internalName,
            "mapper",
            "Ldev/morphia/mapping/Mapper;"
        )
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "dev/morphia/mapping/Mapper",
            "getConfig",
            "()Ldev/morphia/config/MorphiaConfig;",
            false
        )
        mv.visitMethodInsn(
            INVOKEINTERFACE,
            "dev/morphia/config/MorphiaConfig",
            "discriminatorKey",
            "()Ljava/lang/String;",
            true
        )
        mv.visitMethodInsn(
            INVOKESTATIC,
            "dev/morphia/mapping/conventions/MorphiaDefaultsConvention",
            "applyDefaults",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            false
        )
        mv.visitInsn(ARETURN)
        val label1 = label(mv)
        mv.visitLocalVariable(
            "this",
            "Ldev/morphia/critter/sources/ExampleEntityModel;",
            null,
            label0,
            label1,
            0
        )
        mv.visitMaxs(2, 1)
        mv.visitEnd()
    }

    fun useDiscriminator() {
        val mv = classWriter.visitMethod(ACC_PUBLIC, "useDiscriminator", "()Z", null, null)
        mv.visitCode()
        val label0 = label(mv)
        mv.visitLineNumber(137, label0)
        mv.visitInsn(
            if (entity.getAnnotation(Entity::class.java).useDiscriminator) ICONST_1 else ICONST_0
        )
        mv.visitInsn(IRETURN)
        val label1 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    fun getEntityAnnotation() {
        val mv =
            method(
                ACC_PUBLIC,
                "getEntityAnnotation",
                "()Ldev/morphia/annotations/Entity;",
                null,
                null,
                57
            )
        mv.visitCode()
        val label0 = label(mv)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(
            GETFIELD,
            generatedType.internalName,
            "entityAnnotation",
            "Ldev/morphia/annotations/Entity;"
        )
        val label1 = label(mv, visit = false)
        mv.visitJumpInsn(IFNONNULL, label1)
        val label2 = label(mv)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitLdcInsn(Type.getType(entityType.descriptor))
        mv.visitLdcInsn(Type.getType("Ldev/morphia/annotations/Entity;"))
        val label3 = label(mv)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/Class",
            "getAnnotation",
            "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;",
            false
        )
        mv.visitTypeInsn(CHECKCAST, "dev/morphia/annotations/Entity")
        mv.visitMethodInsn(
            INVOKESTATIC,
            "dev/morphia/annotations/internal/EntityBuilder",
            "entityBuilder",
            "(Ldev/morphia/annotations/Entity;)Ldev/morphia/annotations/internal/EntityBuilder;",
            false
        )
        val label4 = label(mv)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "dev/morphia/annotations/internal/EntityBuilder",
            "build",
            "()Ldev/morphia/annotations/Entity;",
            false
        )
        mv.visitFieldInsn(
            PUTFIELD,
            generatedType.internalName,
            "entityAnnotation",
            "Ldev/morphia/annotations/Entity;"
        )
        mv.visitLabel(label1)
        mv.visitLineNumber(62, label1)
        mv.visitFrame(F_SAME, 0, null, 0, null)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(
            GETFIELD,
            generatedType.internalName,
            "entityAnnotation",
            "Ldev/morphia/annotations/Entity;"
        )
        mv.visitInsn(ARETURN)
        val label5 = label(mv)
        mv.visitLocalVariable(
            "this",
            "Ldev/morphia/critter/sources/ExampleEntityModel;",
            null,
            label0,
            label5,
            0
        )
        mv.visitMaxs(3, 1)
        mv.visitEnd()
    }

    fun getType() {
        val mv =
            method(
                ACC_PUBLIC,
                "getType",
                "()Ljava/lang/Class;",
                "()Ljava/lang/Class<*>;",
                null,
                107
            )
        mv.visitCode()
        val label0 = label(mv)
        mv.visitLdcInsn(Type.getType(entityType.descriptor))
        mv.visitInsn(ARETURN)
        val label1 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    private fun isAbstract() {
        val mv = method(ACC_PUBLIC, "isAbstract", "()Z", null, null, 127)
        mv.visitCode()
        val label0 = label(mv)
        mv.visitInsn(if (Modifier.isAbstract(entity.modifiers)) ICONST_1 else ICONST_0)
        mv.visitInsn(IRETURN)
        val label1 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    private fun isInterface() {
        val mv = method(ACC_PUBLIC, "isInterface", "()Z", null, null, 132)
        mv.visitCode()
        val label0 = label(mv)
        mv.visitInsn(if (entity.isInterface) ICONST_1 else ICONST_0)
        mv.visitInsn(IRETURN)
        val label1 = label(mv)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <A : Annotation> ClassNode.getAnnotation(type: Class<A>): A? {
        val node = visibleAnnotations.firstOrNull { a -> a.desc == Type.getType(type).descriptor }
        return node?.let {
            Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                arrayOf(type),
                object : InvocationHandler {
                    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
                        try {
                            node
                            return method.invoke(node) as A
                        } catch (e: Exception) {
                            e.printStackTrace()
                            throw e
                        }
                    }
                }
            ) as A
        }
    }
}
