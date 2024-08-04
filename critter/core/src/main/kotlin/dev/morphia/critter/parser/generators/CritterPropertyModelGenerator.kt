package dev.morphia.critter.parser.generators

import dev.morphia.annotations.internal.AnnotationAsmFactory
import dev.morphia.critter.identifierCase
import dev.morphia.critter.parser.generators.Generators.critterPackage
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Opcodes.PUTFIELD
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Opcodes.V17
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class CritterPropertyModelGenerator(val entity: Class<*>, propertyName: String) :
    BaseGenerator(entity) {
    constructor(entity: Class<*>, field: FieldNode) : this(entity, field.name) {
        annotations = field.visibleAnnotations ?: listOf()
    }

    constructor(
        entity: Class<*>,
        method: MethodNode
    ) : this(entity, method.name.drop(3).identifierCase()) {
        annotations = method.visibleAnnotations ?: listOf()
    }

    companion object {
        private val baseType = Type.getType(CritterPropertyModel::class.java)
    }

    lateinit var methodVisitor: MethodVisitor
    lateinit var fieldVisitor: FieldVisitor
    val accessorType: Type
    lateinit var annotations: List<AnnotationNode>

    init {
        val baseName = "L${critterPackage(entity)}${entity.simpleName}${propertyName.titleCase()}"
        generatedType = Type.getType("${baseName}PropertyModel;")
        accessorType = Type.getType("${baseName}Accessor;")
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

        fields()
        constructor()
        getAccessor()
        //        getAnnotation();
        classWriter.visitEnd()

        return classWriter.toByteArray()
    }

    private fun fields() {
        fieldVisitor =
            classWriter.visitField(
                ACC_PRIVATE,
                "accessor",
                "Lorg/bson/codecs/pojo/PropertyAccessor;",
                "Lorg/bson/codecs/pojo/PropertyAccessor<*>;",
                null
            )
        fieldVisitor.visitEnd()
    }

    private fun getAccessor() {
        methodVisitor =
            classWriter.visitMethod(
                ACC_PUBLIC,
                "getAccessor",
                "()Lorg/bson/codecs/pojo/PropertyAccessor;",
                "()Lorg/bson/codecs/pojo/PropertyAccessor<Ljava/lang/Object;>;",
                null
            )
        methodVisitor.visitCode()
        val label0 = Label()
        methodVisitor.visitLabel(label0)
        methodVisitor.visitLineNumber(22, label0)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitFieldInsn(
            GETFIELD,
            generatedType.internalName,
            "accessor",
            "Lorg/bson/codecs/pojo/PropertyAccessor;"
        )
        methodVisitor.visitInsn(ARETURN)
        val label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLocalVariable("this", generatedType.descriptor, null, label0, label1, 0)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()
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
        val label0 = Label()
        methodVisitor.visitLabel(label0)
        methodVisitor.visitLineNumber(23, label0)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitVarInsn(ALOAD, 1)
        methodVisitor.visitMethodInsn(
            INVOKESPECIAL,
            "dev/morphia/mapping/codec/pojo/critter/CritterPropertyModel",
            "<init>",
            "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
            false
        )
        val label1 = Label()
        methodVisitor.visitLabel(label1)
        methodVisitor.visitLineNumber(20, label1)
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitTypeInsn(NEW, accessorType.internalName)
        methodVisitor.visitInsn(DUP)
        methodVisitor.visitMethodInsn(
            INVOKESPECIAL,
            accessorType.internalName,
            "<init>",
            "()V",
            false
        )
        methodVisitor.visitFieldInsn(
            PUTFIELD,
            generatedType.internalName,
            "accessor",
            "Lorg/bson/codecs/pojo/PropertyAccessor;"
        )

        registerAnnotations()
        val label2 = Label()
        methodVisitor.visitLabel(label2)
        methodVisitor.visitInsn(RETURN)
        val label3 = Label()
        methodVisitor.visitLabel(label3)
        methodVisitor.visitLocalVariable("this", generatedType.descriptor, null, label0, label3, 0)
        methodVisitor.visitLocalVariable(
            "entityModel",
            "Ldev/morphia/mapping/codec/pojo/EntityModel;",
            null,
            label0,
            label3,
            1
        )
        methodVisitor.visitMaxs(3, 2)
        methodVisitor.visitEnd()
    }

    private fun registerAnnotations() {
        annotations.forEach { annotation -> AnnotationAsmFactory.build(methodVisitor, annotation) }
    }

    private fun lookupBuilder(annotation: AnnotationNode): Type {
        val type = Type.getType(annotation.desc)
        val pkg = type.internalName.substringBeforeLast("/")
        val name = type.internalName.substringAfterLast("/")
        return Type.getType("L$pkg/internal/${name}Builder;")
    }
}
