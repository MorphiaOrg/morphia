package dev.morphia.critter.parser.generators

import dev.morphia.critter.parser.java.CritterParser
import dev.morphia.critter.titleCase
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
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
import org.objectweb.asm.tree.ClassNode

class AddFieldAccessorMethods(entity: Class<*>) : BaseGenerator(entity) {
    var fields: Map<String, Type>

    init {
        val cr = ClassReader(entity.name)
        classWriter = ClassWriter(cr, 0)
        cr.accept(classWriter, 0)

        fields = discoverFields()
    }

    private fun discoverFields(): Map<String, Type> {
        val reader = ClassReader(entityType.className)
        val classNode = ClassNode()
        reader.accept(classNode, 0)

        return classNode.fields
            .filter { field ->
                val annotations = field.visibleAnnotations ?: listOf()
                annotations.isEmpty() ||
                    annotations
                        .map { a -> a.desc }
                        .any { desc ->
                            desc in CritterParser.propertyAnnotations() &&
                                desc !in CritterParser.transientAnnotations()
                        }
            }
            .map { field -> field.name to Type.getType(field.desc) }
            .toMap()
    }

    override fun emit(): ByteArray {
        fields.forEach { (name, type) ->
            reader(classWriter, name, type)
            writer(classWriter, name, type)
        }

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }

    private fun writer(classNode: ClassVisitor, field: String, fieldType: Type) {
        val mv =
            classNode.visitMethod(
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

    private fun reader(classNode: ClassVisitor, field: String, fieldType: Type) {
        val mv =
            classNode.visitMethod(
                ACC_PUBLIC or ACC_SYNTHETIC,
                "__read${field.titleCase()}",
                "()${fieldType.descriptor}",
                null,
                null
            )
        mv.visitCode()
        val label0: Label = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(14, label0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitFieldInsn(GETFIELD, entityType.internalName, field, fieldType.descriptor)
        mv.visitInsn(fieldType.getOpcode(IRETURN))
        val label1: Label = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", entityType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }
}
