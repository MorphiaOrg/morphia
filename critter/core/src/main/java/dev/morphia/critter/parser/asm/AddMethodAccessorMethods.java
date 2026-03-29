package dev.morphia.critter.parser.asm;

import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.critter.parser.ExtensionFunctions;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * Generates synthetic {@code __readXxx} and {@code __writeXxx} accessor methods into an entity class
 * bytecode for properties backed by getter/setter methods rather than direct fields.
 */
public class AddMethodAccessorMethods extends BaseGenerator {
    private final Class<?> entity;
    private final List<MethodNode> methods;

    /**
     * Creates a generator that will add accessor methods for the given getter methods to the entity class.
     *
     * @param entity  the entity class to augment
     * @param methods the getter methods for which accessor methods should be generated
     */
    public AddMethodAccessorMethods(Class<?> entity, List<MethodNode> methods) {
        super(entity);
        this.entity = entity;
        this.methods = methods;
        readClassFiltering(entity);
    }

    @Override
    public byte[] emit() {
        for (MethodNode method : methods) {
            String propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
            Type returnType = Type.getReturnType(method.desc);
            reader(propertyName, returnType, method.name);
            writer(propertyName, returnType);
        }
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void writer(String propertyName, Type propertyType) {
        String setterName = "set%s".formatted(Critter.titleCase(propertyName));
        boolean hasSetter = false;
        for (java.lang.reflect.Method m : entity.getMethods()) {
            if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                hasSetter = true;
                break;
            }
        }

        var mv = classWriter.visitMethod(
                ACC_PUBLIC | ACC_SYNTHETIC,
                "__write%s".formatted(Critter.titleCase(propertyName)),
                "(%s)V".formatted(propertyType.getDescriptor()),
                null,
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(18, label0);

        if (hasSetter) {
            // Call the setter method
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(propertyType.getOpcode(ILOAD), 1);
            mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    entityType.getInternalName(),
                    setterName,
                    "(%s)V".formatted(propertyType.getDescriptor()),
                    false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLineNumber(19, label1);
            mv.visitInsn(RETURN);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitLocalVariable("this", entityType.getDescriptor(), null, label0, label2, 0);
            mv.visitLocalVariable("value", propertyType.getDescriptor(), null, label0, label2, 1);
        } else {
            // Throw UnsupportedOperationException for read-only properties
            mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("Property '%s' is read-only".formatted(propertyName));
            mv.visitMethodInsn(
                    INVOKESPECIAL,
                    "java/lang/UnsupportedOperationException",
                    "<init>",
                    "(Ljava/lang/String;)V",
                    false);
            mv.visitInsn(ATHROW);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLocalVariable("this", entityType.getDescriptor(), null, label0, label1, 0);
            mv.visitLocalVariable("value", propertyType.getDescriptor(), null, label0, label1, 1);
        }

        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private void reader(String propertyName, Type returnType, String getterName) {
        String name = "__read%s".formatted(Critter.titleCase(propertyName));
        var mv = classWriter.visitMethod(
                ACC_PUBLIC | ACC_SYNTHETIC,
                name,
                "()%s".formatted(returnType.getDescriptor()),
                null,
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(14, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                entityType.getInternalName(),
                getterName,
                "()%s".formatted(returnType.getDescriptor()),
                false);
        mv.visitInsn(returnType.getOpcode(IRETURN));
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", entityType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
}
