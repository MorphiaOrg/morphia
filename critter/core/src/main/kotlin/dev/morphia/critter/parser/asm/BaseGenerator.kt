package dev.morphia.critter.parser.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SUPER
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type

abstract class BaseGenerator(entity: Class<*>) {
    var classWriter = ClassWriter(COMPUTE_MAXS or COMPUTE_FRAMES)
    val entityType: Type = Type.getType(entity)
    var lineNumber = 0

    lateinit var generatedType: Type

    abstract fun emit(): ByteArray

    protected open fun accessFlags() = ACC_PUBLIC or ACC_SUPER

    /**
     * Reads a class into the classWriter, filtering out any existing __read/__write synthetic
     * methods so that accessor generation is idempotent across repeated plugin runs.
     */
    protected fun readClassFiltering(entity: Class<*>) {
        val resourceName = entity.name.replace('.', '/') + ".class"
        val inputStream =
            entity.classLoader.getResourceAsStream(resourceName)
                ?: throw IllegalArgumentException("Could not find class file for ${entity.name}")
        val filteringVisitor =
            object : ClassVisitor(ASM9, classWriter) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor? {
                    if (
                        (access and ACC_SYNTHETIC != 0) &&
                            (name.startsWith("__read") || name.startsWith("__write"))
                    ) {
                        return null
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions)
                }
            }
        ClassReader(inputStream).accept(filteringVisitor, 0)
    }

    protected fun method(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?,
        lineNumber: Int,
    ): MethodVisitor {
        this.lineNumber = lineNumber
        return classWriter.visitMethod(access, name, descriptor, signature, exceptions)
    }

    protected fun label(
        mv: MethodVisitor,
        lineNumber: Int = this.lineNumber,
        visit: Boolean = true,
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
