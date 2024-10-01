package dev.morphia.critter.parser.asm

import dev.morphia.critter.Critter.Companion.critterPackage
import dev.morphia.critter.parser.Generators.wrap
import dev.morphia.critter.titleCase
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldNode

class EntityAccessorGenerator(entity: Class<*>) : BaseGenerator(entity) {
    constructor(entity: Class<*>, field: FieldNode) : this(entity) {
        propertyName = field.name
        propertyType = Type.getType(field.desc)
        generatedType = Type.getType("L$accessorName;")
    }

    val wrapped: Type by lazy { wrap(propertyType) }
    val accessorName by lazy {
        "${critterPackage(entity)}${entity.simpleName}${propertyName.titleCase()}Accessor"
    }
    lateinit var propertyName: String
    lateinit var propertyType: Type

    override fun emit(): ByteArray {
        classWriter.visit(
            V17,
            accessFlags(),
            generatedType.internalName,
            "Ljava/lang/Object;Lorg/bson/codecs/pojo/PropertyAccessor<${wrap(propertyType).descriptor}>;",
            "java/lang/Object",
            arrayOf("org/bson/codecs/pojo/PropertyAccessor")
        )

        constructor()
        get()
        set()
        setBridge()
        getBridge()
        classWriter.visitEnd()

        return classWriter.toByteArray()
    }

    private fun getBridge() {
        val methodVisitor =
            classWriter.visitMethod(
                ACC_PUBLIC or ACC_BRIDGE or ACC_SYNTHETIC,
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            )
        methodVisitor.visitCode()
        val label0 = Label()
        methodVisitor.visitLabel(label0)
        methodVisitor.visitLineNumber(5, label0)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitVarInsn(ALOAD, 1)
        methodVisitor.visitMethodInsn(
            INVOKEVIRTUAL,
            generatedType.internalName,
            "get",
            "(Ljava/lang/Object;)${wrapped.descriptor}",
            false
        )
        methodVisitor.visitInsn(ARETURN)
        val label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        methodVisitor.visitMaxs(2, 2)
        methodVisitor.visitEnd()
    }

    private fun setBridge() {
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC or ACC_BRIDGE or ACC_SYNTHETIC,
                "set",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(5, label0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitVarInsn(ALOAD, 1)
        mv.visitVarInsn(ALOAD, 2)
        mv.visitTypeInsn(CHECKCAST, wrapped.internalName)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            generatedType.internalName,
            "set",
            "(Ljava/lang/Object;${wrapped.descriptor})V",
            false
        )
        mv.visitInsn(RETURN)
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(3, 3)
        mv.visitEnd()
    }

    private fun set() {
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC,
                "set",
                "(Ljava/lang/Object;${wrapped.descriptor})V",
                "<S:Ljava/lang/Object;>(TS;${wrapped.descriptor})V",
                null
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(13, label0)
        mv.visitVarInsn(ALOAD, 1)
        mv.visitTypeInsn(CHECKCAST, entityType.internalName)
        mv.visitVarInsn(ALOAD, 2)
        if (!wrapped.equals(propertyType)) {
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                wrapped.internalName,
                "${propertyType.className}Value",
                "()${propertyType.descriptor}",
                false
            )
        }
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            entityType.internalName,
            "__write${propertyName.titleCase()}",
            "(${propertyType.descriptor})V",
            false
        )
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLineNumber(14, label1)
        mv.visitInsn(RETURN)
        val label2 = Label()
        mv.visitLabel(label2)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label2, 0)
        mv.visitLocalVariable("entity", "Ljava/lang/Object;", "TS;", label0, label2, 1)
        mv.visitLocalVariable("value", propertyType.descriptor, null, label0, label2, 2)
        mv.visitMaxs(2, 3)
        mv.visitEnd()
    }

    private fun get() {
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC,
                "get",
                "(Ljava/lang/Object;)${wrapped.descriptor}",
                "<S:Ljava/lang/Object;>(TS;)${wrapped.descriptor}",
                null
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(8, label0)
        mv.visitVarInsn(ALOAD, 1)
        mv.visitTypeInsn(CHECKCAST, entityType.internalName)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            entityType.internalName,
            "__read${propertyName.titleCase()}",
            "()${propertyType.descriptor}",
            false
        )
        if (!wrapped.equals(propertyType)) {
            mv.visitMethodInsn(
                INVOKESTATIC,
                wrapped.internalName,
                "valueOf",
                "(${propertyType.descriptor})${wrapped.descriptor}",
                false
            )
        }
        mv.visitInsn(ARETURN)
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        mv.visitLocalVariable("entity", "Ljava/lang/Object;", "TS;", label0, label1, 1)
        mv.visitMaxs(1, 2)
        mv.visitEnd()
    }

    fun constructor() {
        val methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        methodVisitor.visitCode()
        val label0 = Label()
        methodVisitor.visitLabel(label0)
        methodVisitor.visitLineNumber(5, label0)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        methodVisitor.visitInsn(RETURN)
        val label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()
    }
}
