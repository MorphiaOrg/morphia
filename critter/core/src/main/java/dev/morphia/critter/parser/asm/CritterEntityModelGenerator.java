package dev.morphia.critter.parser.asm;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.critter.Critter;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

public class CritterEntityModelGenerator extends BaseGenerator {
    private static final Type BASE_TYPE = Type.getType(CritterEntityModel.class);
    private final Class<?> entity;
    private final List<String> models;

    public CritterEntityModelGenerator(Class<?> entity, List<String> models) {
        super(entity);
        this.entity = entity;
        this.models = models;
        String generatorName = Critter.critterPackage(entity) + "." + entity.getSimpleName() + "EntityModel";
        generatedType = Type.getType("L" + generatorName.replace('.', '/') + ";");
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

        constructor();
        collectionName();
        discriminator();
        discriminatorKey();
        useDiscriminator();
        getEntityAnnotation();
        getType();
        isAbstract();
        isInterface();

        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    public void constructor() {
        var mv = method(ACC_PUBLIC, "<init>", "(Ldev/morphia/mapping/Mapper;)V", null, null, 24);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(Type.getType(entityType.getDescriptor()));
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "dev/morphia/critter/CritterEntityModel",
                "<init>",
                "(Ldev/morphia/mapping/Mapper;Ljava/lang/Class;)V",
                false);

        propertyModels(mv, models);

        var label1 = label(mv);
        mv.visitInsn(RETURN);
        var label2 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label2, 0);
        mv.visitLocalVariable("mapper", "Ldev/morphia/mapping/Mapper;", null, label0, label2, 1);
        mv.visitEnd();
    }

    private void propertyModels(MethodVisitor mv, List<String> models) {
        for (String it : models) {
            String model = it.replace('.', '/');
            label(mv);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, model);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(
                    INVOKESPECIAL,
                    model,
                    "<init>",
                    "(Ldev/morphia/mapping/codec/pojo/EntityModel;)V",
                    false);
            mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    generatedType.getInternalName(),
                    "addProperty",
                    "(Ldev/morphia/mapping/codec/pojo/PropertyModel;)Z",
                    false);
            mv.visitInsn(POP);
        }
    }

    public void collectionName() {
        var mv = method(ACC_PUBLIC, "collectionName", "()Ljava/lang/String;", null, null, 37);
        mv.visitCode();
        var label0 = label(mv, 37);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, generatedType.getInternalName(), "mapper", "Ldev/morphia/mapping/Mapper;");
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "dev/morphia/mapping/Mapper",
                "getConfig",
                "()Ldev/morphia/config/MorphiaConfig;",
                false);
        mv.visitMethodInsn(
                INVOKEINTERFACE,
                "dev/morphia/config/MorphiaConfig",
                "collectionNaming",
                "()Ldev/morphia/mapping/NamingStrategy;",
                true);
        mv.visitLdcInsn(entityType.getInternalName().substring(entityType.getInternalName().lastIndexOf('/') + 1));
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "dev/morphia/mapping/NamingStrategy",
                "apply",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(ARETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitEnd();
    }

    public void discriminator() {
        var mv = method(ACC_PUBLIC, "discriminator", "()Ljava/lang/String;", null, null, 47);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, generatedType.getInternalName(), "mapper", "Ldev/morphia/mapping/Mapper;");
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "dev/morphia/mapping/Mapper",
                "getConfig",
                "()Ldev/morphia/config/MorphiaConfig;",
                false);
        mv.visitMethodInsn(
                INVOKEINTERFACE,
                "dev/morphia/config/MorphiaConfig",
                "discriminator",
                "()Ldev/morphia/mapping/DiscriminatorFunction;",
                true);
        mv.visitLdcInsn(entityType);
        mv.visitLdcInsn(entity.getAnnotation(Entity.class).discriminator());
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "dev/morphia/mapping/DiscriminatorFunction",
                "apply",
                "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(ARETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    public void discriminatorKey() {
        var mv = method(ACC_PUBLIC, "discriminatorKey", "()Ljava/lang/String;", null, null, 52);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitLdcInsn(entity.getAnnotation(Entity.class).discriminatorKey());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, generatedType.getInternalName(), "mapper", "Ldev/morphia/mapping/Mapper;");
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "dev/morphia/mapping/Mapper",
                "getConfig",
                "()Ldev/morphia/config/MorphiaConfig;",
                false);
        mv.visitMethodInsn(
                INVOKEINTERFACE,
                "dev/morphia/config/MorphiaConfig",
                "discriminatorKey",
                "()Ljava/lang/String;",
                true);
        mv.visitMethodInsn(
                INVOKESTATIC,
                "dev/morphia/mapping/conventions/MorphiaDefaultsConvention",
                "applyDefaults",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(ARETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    public void useDiscriminator() {
        var mv = classWriter.visitMethod(ACC_PUBLIC, "useDiscriminator", "()Z", null, null);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitLineNumber(137, label0);
        mv.visitInsn(entity.getAnnotation(Entity.class).useDiscriminator() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public void getEntityAnnotation() {
        var mv = method(ACC_PUBLIC, "getEntityAnnotation", "()Ldev/morphia/annotations/Entity;", null, null, 57);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, generatedType.getInternalName(), "entityAnnotation", "Ldev/morphia/annotations/Entity;");
        var label1 = new org.objectweb.asm.Label();
        mv.visitJumpInsn(IFNONNULL, label1);
        var label2 = label(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(Type.getType(entityType.getDescriptor()));
        mv.visitLdcInsn(Type.getType("Ldev/morphia/annotations/Entity;"));
        var label3 = label(mv);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/lang/Class",
                "getAnnotation",
                "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;",
                false);
        mv.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "dev/morphia/annotations/Entity");
        mv.visitMethodInsn(
                INVOKESTATIC,
                "dev/morphia/annotations/internal/EntityBuilder",
                "entityBuilder",
                "(Ldev/morphia/annotations/Entity;)Ldev/morphia/annotations/internal/EntityBuilder;",
                false);
        var label4 = label(mv);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "dev/morphia/annotations/internal/EntityBuilder",
                "build",
                "()Ldev/morphia/annotations/Entity;",
                false);
        mv.visitFieldInsn(PUTFIELD, generatedType.getInternalName(), "entityAnnotation", "Ldev/morphia/annotations/Entity;");
        mv.visitLabel(label1);
        mv.visitLineNumber(62, label1);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, generatedType.getInternalName(), "entityAnnotation", "Ldev/morphia/annotations/Entity;");
        mv.visitInsn(ARETURN);
        var label5 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label5, 0);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    public void getType() {
        var mv = method(ACC_PUBLIC, "getType", "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null, 107);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitLdcInsn(Type.getType(entityType.getDescriptor()));
        mv.visitInsn(ARETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void isAbstract() {
        var mv = method(ACC_PUBLIC, "isAbstract", "()Z", null, null, 127);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitInsn(java.lang.reflect.Modifier.isAbstract(entity.getModifiers()) ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void isInterface() {
        var mv = method(ACC_PUBLIC, "isInterface", "()Z", null, null, 132);
        mv.visitCode();
        var label0 = label(mv);
        mv.visitInsn(entity.isInterface() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        var label1 = label(mv);
        mv.visitLocalVariable("this", generatedType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
}
