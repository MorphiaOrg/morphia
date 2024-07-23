package dev.morphia.critter.parser.generators

import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Opcodes.V17
import org.objectweb.asm.Type

class CritterPropertyModelGenerator(val entity: Class<*>, propertyName: String) :
    BaseGenerator(entity) {
    companion object {
        private val baseType = Type.getType(CritterPropertyModel::class.java)
    }

    init {
        val generatorName =
            "${critterPackage(entity)}${entity.simpleName}${propertyName.titleCase()}PropertyModel"
        generatedType = Type.getType("L${generatorName};")
    }

    lateinit var methodVisitor: MethodVisitor

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

        classWriter.visitEnd()

        return classWriter.toByteArray()
    }

    private fun constructor() {
        methodVisitor =
            classWriter.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
                null,
                null
            )
        methodVisitor.visitCode()
        var label0 = Label()
        methodVisitor.visitLabel(label0)
        methodVisitor.visitLineNumber(14, label0)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitVarInsn(ALOAD, 1)
        methodVisitor.visitMethodInsn(
            INVOKESPECIAL,
            "dev/morphia/mapping/codec/pojo/critter/CritterPropertyModel",
            "<init>",
            "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
            false
        )
        var label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLineNumber(15, label1)
        methodVisitor.visitInsn(RETURN)
        var label2 = Label()
        methodVisitor.visitLabel(label2)
        methodVisitor.visitLocalVariable("this", generatedType.descriptor, null, label0, label2, 0)
        methodVisitor.visitLocalVariable(
            "entityModel",
            "Ldev/morphia/mapping/codec/pojo/EntityModel;",
            null,
            label0,
            label2,
            1
        )
        methodVisitor.visitMaxs(2, 2)
        methodVisitor.visitEnd()
    }
}
