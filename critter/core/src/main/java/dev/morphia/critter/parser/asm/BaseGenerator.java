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

/**
 * Base class for ASM-based bytecode generators used by Critter to produce accessor and model classes.
 */
public abstract class BaseGenerator {
    /** The ASM {@link ClassWriter} used to assemble the generated class bytecode. */
    protected ClassWriter classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
    /** The ASM type of the entity class being processed. */
    protected final Type entityType;
    /** The current source line number used when emitting debug line-number instructions. */
    protected int lineNumber = 0;
    /** The ASM type of the class being generated. */
    protected Type generatedType;

    /**
     * Creates a new generator for the given entity class.
     *
     * @param entity the entity class whose bytecode will be augmented or used as a template
     */
    protected BaseGenerator(Class<?> entity) {
        this.entityType = Type.getType(entity);
    }

    /**
     * Emits the generated or augmented class bytecode.
     *
     * @return the bytecode of the generated class
     */
    public abstract byte[] emit();

    /**
     * Returns the access flags used when declaring the generated class.
     *
     * @return the ACC_PUBLIC | ACC_SUPER access flags
     */
    protected int accessFlags() {
        return ACC_PUBLIC | ACC_SUPER;
    }

    /**
     * Reads a class into the classWriter, filtering out any existing __read/__write synthetic
     * methods so that accessor generation is idempotent across repeated plugin runs.
     *
     * @param entity the class to read
     */
    protected void readClassFiltering(Class<?> entity) {
        String resourceName = "%s.class".formatted(entity.getName().replace('.', '/'));
        java.io.InputStream inputStream = entity.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find class file for %s".formatted(entity.getName()));
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
            throw new RuntimeException("Failed to read class %s".formatted(entity.getName()), e);
        }
    }

    /**
     * Visits a method on the class writer and records the starting line number.
     *
     * @param access     the method access flags
     * @param name       the method name
     * @param descriptor the method descriptor
     * @param signature  the generic signature, or {@code null}
     * @param exceptions the internal names of thrown exception types, or {@code null}
     * @param lineNumber the source line number to associate with this method
     * @return the {@link MethodVisitor} for the new method
     */
    protected MethodVisitor method(int access, String name, String descriptor, String signature,
            String[] exceptions, int lineNumber) {
        this.lineNumber = lineNumber;
        return classWriter.visitMethod(access, name, descriptor, signature, exceptions);
    }

    /**
     * Creates a new label, visits it, and emits a line-number debug instruction.
     *
     * @param mv         the method visitor
     * @param lineNumber the line number to associate with the label
     * @return the created label
     */
    protected Label label(MethodVisitor mv, int lineNumber) {
        return label(mv, lineNumber, true);
    }

    /**
     * Creates a new label, visits it, and emits a line-number debug instruction using the current line number.
     *
     * @param mv the method visitor
     * @return the created label
     */
    protected Label label(MethodVisitor mv) {
        return label(mv, this.lineNumber, true);
    }

    /**
     * Creates a new label and optionally visits it with a line-number debug instruction.
     *
     * @param mv         the method visitor
     * @param lineNumber the line number to associate with the label
     * @param visit      {@code true} to visit the label and emit the line number instruction
     * @return the created label
     */
    protected Label label(MethodVisitor mv, int lineNumber, boolean visit) {
        Label label0 = new Label();
        if (visit) {
            mv.visitLabel(label0);
            mv.visitLineNumber(lineNumber, label0);
            this.lineNumber = lineNumber + 1;
        }
        return label0;
    }

    /**
     * Creates a new label without visiting it on the method visitor.
     *
     * @param mv the method visitor (unused; present for API consistency)
     * @return the created label
     */
    protected Label labelNoVisit(MethodVisitor mv) {
        Label label0 = new Label();
        return label0;
    }
}
