package dev.morphia.critter.parser.generators

import dev.morphia.critter.parser.titleCase
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

class EntityAccessorGenerator(host: Class<*>, val fieldName: String, fieldClass: Class<*>) {
    val entityType: Type = Type.getType(host)
    val fieldType = Type.getType(fieldClass)
    val classWriter = ClassWriter(0)
    val accessorName =
        "${host.packageName.replace('.', '/')}/__morphia/${host.simpleName}${fieldName.titleCase()}Accessor"
    val accessorType = Type.getType("L$accessorName;")

    fun dump(): ByteArray {
        classWriter.visit(
            V17,
            ACC_PUBLIC or ACC_SUPER,
            accessorType.internalName,
            "Ljava/lang/Object;Lorg/bson/codecs/pojo/PropertyAccessor<${fieldType.descriptor}>;",
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
            accessorType.internalName,
            "get",
            "(Ljava/lang/Object;)${fieldType.descriptor}",
            false
        )
        methodVisitor.visitInsn(ARETURN)
        val label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLocalVariable("this", accessorType.descriptor, null, label0, label1, 0)
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
        mv.visitTypeInsn(CHECKCAST, fieldType.internalName)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            accessorType.internalName,
            "set",
            "(Ljava/lang/Object;${fieldType.descriptor})V",
            false
        )
        mv.visitInsn(RETURN)
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", accessorType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(3, 3)
        mv.visitEnd()
    }

    private fun set() {
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC,
                "set",
                "(Ljava/lang/Object;${fieldType.descriptor})V",
                "<S:Ljava/lang/Object;>(TS;${fieldType.descriptor})V",
                null
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(13, label0)
        mv.visitVarInsn(ALOAD, 1)
        mv.visitTypeInsn(CHECKCAST, entityType.internalName)
        mv.visitVarInsn(ALOAD, 2)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            entityType.internalName,
            "__writeName",
            "(${fieldType.descriptor})V",
            false
        )
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLineNumber(14, label1)
        mv.visitInsn(RETURN)
        val label2 = Label()
        mv.visitLabel(label2)
        mv.visitLocalVariable("this", accessorType.descriptor, null, label0, label2, 0)
        mv.visitLocalVariable("entity", "Ljava/lang/Object;", "TS;", label0, label2, 1)
        mv.visitLocalVariable("value", fieldType.descriptor, null, label0, label2, 2)
        mv.visitMaxs(2, 3)
        mv.visitEnd()
    }

    private fun get() {
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC,
                "get",
                "(Ljava/lang/Object;)${fieldType.descriptor}",
                "<S:Ljava/lang/Object;>(TS;)${fieldType.descriptor}",
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
            "__readName",
            "()${fieldType.descriptor}",
            false
        )
        mv.visitInsn(ARETURN)
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", accessorType.descriptor, null, label0, label1, 0)
        mv.visitLocalVariable("entity", "Ljava/lang/Object;", "TS;", label0, label1, 1)
        mv.visitMaxs(1, 2)
        mv.visitEnd()
    }

    private fun constructor() {
        val methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        methodVisitor.visitCode()
        val label0 = Label()
        methodVisitor.visitLabel(label0)
        methodVisitor.visitLineNumber(15, label0)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        methodVisitor.visitInsn(RETURN)
        val label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLocalVariable("this", accessorType.descriptor, null, label0, label1, 0)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()
    }
}
