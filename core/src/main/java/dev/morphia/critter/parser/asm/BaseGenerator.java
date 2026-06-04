package dev.morphia.critter.parser.asm;

import java.io.IOException;
import java.io.InputStream;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;

/**
 * Base class for bytecode generators that read and transform existing class files.
 */
public abstract class BaseGenerator {
    /** The entity class whose bytecode will be augmented. */
    protected final Class<?> entity;

    /**
     * Creates a new generator for the given entity class.
     */
    protected BaseGenerator(Class<?> entity) {
        this.entity = entity;
    }

    /**
     * Emits the generated or augmented class bytecode.
     */
    public abstract byte[] emit();

    /**
     * Reads the class file bytes for the given entity, excluding any existing __read/__write synthetic methods.
     */
    protected ClassModel readClassFiltering() {
        String resourceName = "%s.class".formatted(entity.getName().replace('.', '/'));
        InputStream inputStream = entity.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find class file for %s".formatted(entity.getName()));
        }
        try {
            byte[] bytes = inputStream.readAllBytes();
            ClassModel model = ClassFile.of().parse(bytes);
            return model;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class %s".formatted(entity.getName()), e);
        }
    }
}
