package dev.morphia.critter.parser.generators

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

abstract class BaseGenerator(entity: Class<*>) {
    var classWriter = ClassWriter(0)
    val entityType: Type = Type.getType(entity)

    lateinit var generatedType: Type

    abstract fun emit(): ByteArray

    protected open fun accessFlags() = ACC_PUBLIC or ACC_SUPER

    protected fun critterPackage(entity: Class<*>) =
        "${entity.packageName.replace('.', '/')}/__morphia/"
}
