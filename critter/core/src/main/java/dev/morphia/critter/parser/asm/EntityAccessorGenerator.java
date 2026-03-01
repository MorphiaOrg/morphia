package dev.morphia.critter.parser.asm;

import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.Generators;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

public class EntityAccessorGenerator extends BaseGenerator {
    private final String propertyName;
    private final Type propertyType;
    private final Type wrapped;

    public EntityAccessorGenerator(Class<?> entity, FieldNode field) {
        super(entity);
        this.propertyName = field.name;
        this.propertyType = Type.getType(field.desc);
        this.wrapped = Generators.wrap(propertyType);
        String accessorName = Critter.critterPackage(entity) + entity.getSimpleName() + Critter.titleCase(propertyName) + "Accessor";
        generatedType = Type.getType("L" + accessorName.replace('.', '/') + ";");
    }

    @Override
    public byte[] emit() {
        classWriter.visit(
                V17,
                accessFlags(),
                generatedType.getInternalName(),
                "Ljava/lang/Object;Lorg/bson/codecs/pojo/PropertyAccessor<" + Generators.wrap(propertyType).getDescriptor() + ">;",
                "java/lang/Object",
                new String[] { "org/bson/codecs/pojo/PropertyAccessor" });

        constructor();
        get();
        set();
        setBridge();
        getBridge();
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void getBridge() {
        var mv = classWriter.visitMethod(
                ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(5, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                generatedType.getInternalName(),
                "get",
                "(Ljava/lang/Object;)" + wrapped.getDescriptor(),
                false);
        mv.visitInsn(ARETURN);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private void setBridge() {
        var mv = classWriter.visitMethod(
                ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
                "set",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(5, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, wrapped.getInternalName());
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                generatedType.getInternalName(),
                "set",
                "(Ljava/lang/Object;" + wrapped.getDescriptor() + ")V",
                false);
        mv.visitInsn(RETURN);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private void set() {
        var mv = classWriter.visitMethod(
                ACC_PUBLIC,
                "set",
                "(Ljava/lang/Object;" + wrapped.getDescriptor() + ")V",
                "<S:Ljava/lang/Object;>(TS;" + wrapped.getDescriptor() + ")V",
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(13, label0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, entityType.getInternalName());
        mv.visitVarInsn(ALOAD, 2);
        if (!wrapped.equals(propertyType)) {
            mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    wrapped.getInternalName(),
                    propertyType.getClassName() + "Value",
                    "()" + propertyType.getDescriptor(),
                    false);
        }
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                entityType.getInternalName(),
                "__write" + Critter.titleCase(propertyName),
                "(" + propertyType.getDescriptor() + ")V",
                false);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLineNumber(14, label1);
        mv.visitInsn(RETURN);
        Label label2 = new Label();
        mv.visitLabel(label2);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label2, 0);
        mv.visitLocalVariable("entity", "Ljava/lang/Object;", "TS;", label0, label2, 1);
        mv.visitLocalVariable("value", propertyType.getDescriptor(), null, label0, label2, 2);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
    }

    private void get() {
        var mv = classWriter.visitMethod(
                ACC_PUBLIC,
                "get",
                "(Ljava/lang/Object;)" + wrapped.getDescriptor(),
                "<S:Ljava/lang/Object;>(TS;)" + wrapped.getDescriptor(),
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(8, label0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, entityType.getInternalName());
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                entityType.getInternalName(),
                "__read" + Critter.titleCase(propertyName),
                "()" + propertyType.getDescriptor(),
                false);
        if (!wrapped.equals(propertyType)) {
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    wrapped.getInternalName(),
                    "valueOf",
                    "(" + propertyType.getDescriptor() + ")" + wrapped.getDescriptor(),
                    false);
        }
        mv.visitInsn(ARETURN);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitLocalVariable("entity", "Ljava/lang/Object;", "TS;", label0, label1, 1);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    public void constructor() {
        var mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(5, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
}
