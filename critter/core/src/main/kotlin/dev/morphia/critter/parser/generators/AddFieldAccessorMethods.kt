package dev.morphia.critter.parser.generators

import dev.morphia.critter.titleCase
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.ILOAD
import org.objectweb.asm.Opcodes.IRETURN
import org.objectweb.asm.Opcodes.PUTFIELD
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldNode

class AddFieldAccessorMethods(entity: Class<*>, var fields: List<FieldNode>) :
    BaseGenerator(entity) {

    init {
        ClassReader(entity.name).accept(classWriter, 0)
    }

    override fun emit(): ByteArray {
        fields.forEach { field ->
            val name = field.name
            val type = Type.getType(field.desc)
            reader(name, type)
            writer(name, type)
        }

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }

    private fun writer(field: String, fieldType: Type) {
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC or ACC_SYNTHETIC,
                "__write${field.titleCase()}",
                "(${fieldType.descriptor})V",
                null,
                null
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(18, label0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitVarInsn(fieldType.getOpcode(ILOAD), 1)
        mv.visitFieldInsn(PUTFIELD, entityType.internalName, field, fieldType.descriptor)
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLineNumber(19, label1)
        mv.visitInsn(RETURN)
        val label2 = Label()
        mv.visitLabel(label2)
        mv.visitLocalVariable("this", entityType.descriptor, null, label0, label2, 0)
        mv.visitLocalVariable("value", fieldType.descriptor, null, label0, label2, 1)
        mv.visitMaxs(2, 2)
        mv.visitEnd()
    }

    private fun reader(field: String, fieldType: Type) {
        val name = "__read${field.titleCase()}"
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC or ACC_SYNTHETIC,
                name,
                "()${fieldType.descriptor}",
                null,
                null
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(14, label0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(GETFIELD, entityType.internalName, field, fieldType.descriptor)
        mv.visitInsn(fieldType.getOpcode(IRETURN))
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", entityType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }
}
