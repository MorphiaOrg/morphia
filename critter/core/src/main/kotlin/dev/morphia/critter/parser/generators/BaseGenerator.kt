package dev.morphia.critter.parser.generators

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SUPER
import org.objectweb.asm.Type

abstract class BaseGenerator(entity: Class<*>) {
    var classWriter = ClassWriter(COMPUTE_MAXS or COMPUTE_FRAMES)
    val entityType: Type = Type.getType(entity)
    var lineNumber = 0

    lateinit var generatedType: Type

    abstract fun emit(): ByteArray

    protected open fun accessFlags() = ACC_PUBLIC or ACC_SUPER

    protected fun method(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?,
        lineNumber: Int
    ): MethodVisitor {
        this.lineNumber = lineNumber
        return classWriter.visitMethod(access, name, descriptor, signature, exceptions)
    }

    protected fun label(
        mv: MethodVisitor,
        lineNumber: Int = this.lineNumber,
        visit: Boolean = true
    ): Label {
        val label0 = Label()
        if (visit) {
            mv.visitLabel(label0)
            mv.visitLineNumber(lineNumber, label0)
            this.lineNumber = lineNumber + 1
        }

        return label0
    }
}
