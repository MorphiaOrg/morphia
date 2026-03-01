package dev.morphia.critter.parser.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM9;

public abstract class BaseGenerator {
    protected ClassWriter classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
    protected final Type entityType;
    protected int lineNumber = 0;
    protected Type generatedType;

    protected BaseGenerator(Class<?> entity) {
        this.entityType = Type.getType(entity);
    }

    public abstract byte[] emit();

    protected int accessFlags() {
        return ACC_PUBLIC | ACC_SUPER;
    }

    /**
     * Reads a class into the classWriter, filtering out any existing __read/__write synthetic
     * methods so that accessor generation is idempotent across repeated plugin runs.
     */
    protected void readClassFiltering(Class<?> entity) {
        String resourceName = entity.getName().replace('.', '/') + ".class";
        java.io.InputStream inputStream = entity.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find class file for " + entity.getName());
        }
        ClassVisitor filteringVisitor = new ClassVisitor(ASM9, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                if ((access & ACC_SYNTHETIC) != 0
                        && (name.startsWith("__read") || name.startsWith("__write"))) {
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        try {
            new ClassReader(inputStream).accept(filteringVisitor, 0);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read class " + entity.getName(), e);
        }
    }

    protected MethodVisitor method(int access, String name, String descriptor, String signature,
            String[] exceptions, int lineNumber) {
        this.lineNumber = lineNumber;
        return classWriter.visitMethod(access, name, descriptor, signature, exceptions);
    }

    protected Label label(MethodVisitor mv, int lineNumber) {
        return label(mv, lineNumber, true);
    }

    protected Label label(MethodVisitor mv) {
        return label(mv, this.lineNumber, true);
    }

    protected Label label(MethodVisitor mv, int lineNumber, boolean visit) {
        Label label0 = new Label();
        if (visit) {
            mv.visitLabel(label0);
            mv.visitLineNumber(lineNumber, label0);
            this.lineNumber = lineNumber + 1;
        }
        return label0;
    }

    protected Label labelNoVisit(MethodVisitor mv) {
        Label label0 = new Label();
        return label0;
    }
}
