package dev.morphia.critter.parser.asm;

import java.util.List;

import dev.morphia.critter.Critter;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

public class AddFieldAccessorMethods extends BaseGenerator {
    private final List<FieldNode> fields;

    public AddFieldAccessorMethods(Class<?> entity, List<FieldNode> fields) {
        super(entity);
        this.fields = fields;
        readClassFiltering(entity);
    }

    @Override
    public byte[] emit() {
        for (FieldNode field : fields) {
            String name = field.name;
            Type type = Type.getType(field.desc);
            reader(name, type);
            writer(name, type);
        }
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void writer(String field, Type fieldType) {
        var mv = classWriter.visitMethod(
                ACC_PUBLIC | ACC_SYNTHETIC,
                "__write%s".formatted(Critter.titleCase(field)),
                "(%s)V".formatted(fieldType.getDescriptor()),
                null,
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(18, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(fieldType.getOpcode(ILOAD), 1);
        mv.visitFieldInsn(PUTFIELD, entityType.getInternalName(), field, fieldType.getDescriptor());
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLineNumber(19, label1);
        mv.visitInsn(RETURN);
        Label label2 = new Label();
        mv.visitLabel(label2);
        mv.visitLocalVariable("this", entityType.getDescriptor(), null, label0, label2, 0);
        mv.visitLocalVariable("value", fieldType.getDescriptor(), null, label0, label2, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private void reader(String field, Type fieldType) {
        String name = "__read%s".formatted(Critter.titleCase(field));
        var mv = classWriter.visitMethod(
                ACC_PUBLIC | ACC_SYNTHETIC,
                name,
                "()%s".formatted(fieldType.getDescriptor()),
                null,
                null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(14, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, entityType.getInternalName(), field, fieldType.getDescriptor());
        mv.visitInsn(fieldType.getOpcode(IRETURN));
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLocalVariable("this", entityType.getDescriptor(), null, label0, label1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
}
