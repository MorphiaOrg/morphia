package dev.morphia.critter.parser.asm;

import java.util.Collections;
import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

public class CritterPropertyModelGenerator extends BaseGenerator {
    private static final Type BASE_TYPE = Type.getType(CritterPropertyModel.class);

    private MethodVisitor methodVisitor;
    private FieldVisitor fieldVisitor;
    private final Type accessorType;
    private List<AnnotationNode> annotations;

    public CritterPropertyModelGenerator(Class<?> entity, FieldNode field) {
        this(entity, field.name);
        this.annotations = field.visibleAnnotations != null ? field.visibleAnnotations : Collections.emptyList();
    }

    public CritterPropertyModelGenerator(Class<?> entity, MethodNode method) {
        this(entity, Critter.identifierCase(method.name.substring(3)));
        this.annotations = method.visibleAnnotations != null ? method.visibleAnnotations : Collections.emptyList();
    }

    private CritterPropertyModelGenerator(Class<?> entity, String propertyName) {
        super(entity);
        String baseName = "L" + Critter.critterPackage(entity).replace('.', '/') + entity.getSimpleName() + Critter.titleCase(propertyName);
        generatedType = Type.getType(baseName + "PropertyModel;");
        accessorType = Type.getType(baseName + "Accessor;");
    }

    @Override
    public byte[] emit() {
        classWriter.visit(
                V17,
                accessFlags(),
                generatedType.getInternalName(),
                null,
                BASE_TYPE.getInternalName(),
                null);

        fields();
        constructor();
        getAccessor();
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void fields() {
        fieldVisitor = classWriter.visitField(
                ACC_PRIVATE,
                "accessor",
                "Lorg/bson/codecs/pojo/PropertyAccessor;",
                "Lorg/bson/codecs/pojo/PropertyAccessor<*>;",
                null);
        fieldVisitor.visitEnd();
    }

    private void getAccessor() {
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC,
                "getAccessor",
                "()Lorg/bson/codecs/pojo/PropertyAccessor;",
                "()Lorg/bson/codecs/pojo/PropertyAccessor<Ljava/lang/Object;>;",
                null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(22, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedType.getInternalName(), "accessor", "Lorg/bson/codecs/pojo/PropertyAccessor;");
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
    }

    private void constructor() {
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
                null,
                null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(23, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(
                INVOKESPECIAL,
                "dev/morphia/mapping/codec/pojo/critter/CritterPropertyModel",
                "<init>",
                "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
                false);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(20, label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitTypeInsn(NEW, accessorType.getInternalName());
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, accessorType.getInternalName(), "<init>", "()V", false);
        methodVisitor.visitFieldInsn(PUTFIELD, generatedType.getInternalName(), "accessor", "Lorg/bson/codecs/pojo/PropertyAccessor;");
        registerAnnotations();
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitInsn(RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label3, 0);
        methodVisitor.visitLocalVariable("entityModel", "Ldev/morphia/mapping/codec/pojo/EntityModel;", null, label0, label3, 1);
        methodVisitor.visitMaxs(3, 2);
        methodVisitor.visitEnd();
    }

    private void registerAnnotations() {
        // Annotations registered later via Gizmo-based approach
    }
}
