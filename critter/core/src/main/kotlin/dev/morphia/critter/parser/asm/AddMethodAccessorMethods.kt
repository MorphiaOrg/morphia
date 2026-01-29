package dev.morphia.critter.parser.asm

import dev.morphia.critter.parser.methodCase
import dev.morphia.critter.titleCase
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ILOAD
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.Opcodes.IRETURN
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

class AddMethodAccessorMethods(entity: Class<*>, var methods: List<MethodNode>) :
    BaseGenerator(entity) {

    init {
        val resourceName = entity.name.replace('.', '/') + ".class"
        val inputStream =
            entity.classLoader.getResourceAsStream(resourceName)
                ?: throw IllegalArgumentException("Could not find class file for ${entity.name}")
        ClassReader(inputStream).accept(classWriter, 0)
    }

    override fun emit(): ByteArray {
        methods.forEach { method ->
            val propertyName = method.name.methodCase()
            val returnType = Type.getReturnType(method.desc)
            reader(propertyName, returnType, method.name)
            writer(propertyName, returnType)
        }

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }

    private fun writer(propertyName: String, propertyType: Type) {
        // Find the setter method name (setXxx)
        val setterName = "set${propertyName.titleCase()}"

        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC or ACC_SYNTHETIC,
                "__write${propertyName.titleCase()}",
                "(${propertyType.descriptor})V",
                null,
                null,
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(18, label0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitVarInsn(propertyType.getOpcode(ILOAD), 1)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            entityType.internalName,
            setterName,
            "(${propertyType.descriptor})V",
            false,
        )
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLineNumber(19, label1)
        mv.visitInsn(RETURN)
        val label2 = Label()
        mv.visitLabel(label2)
        mv.visitLocalVariable("this", entityType.descriptor, null, label0, label2, 0)
        mv.visitLocalVariable("value", propertyType.descriptor, null, label0, label2, 1)
        mv.visitMaxs(2, 2)
        mv.visitEnd()
    }

    private fun reader(propertyName: String, returnType: Type, getterName: String) {
        val name = "__read${propertyName.titleCase()}"
        val mv =
            classWriter.visitMethod(
                ACC_PUBLIC or ACC_SYNTHETIC,
                name,
                "()${returnType.descriptor}",
                null,
                null,
            )
        mv.visitCode()
        val label0 = Label()
        mv.visitLabel(label0)
        mv.visitLineNumber(14, label0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            entityType.internalName,
            getterName,
            "()${returnType.descriptor}",
            false,
        )
        mv.visitInsn(returnType.getOpcode(IRETURN))
        val label1 = Label()
        mv.visitLabel(label1)
        mv.visitLocalVariable("this", entityType.descriptor, null, label0, label1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }
}
