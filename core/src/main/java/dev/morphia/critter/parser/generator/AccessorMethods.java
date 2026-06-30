package dev.morphia.critter.parser.generator;

import dev.morphia.mapping.MappingException;

import io.github.dmlloyd.classfile.ClassModel;

/**
 * Base class for bytecode generators that read and transform existing class files.
 */
public abstract class AccessorMethods {
    /** The entity class whose bytecode will be augmented. */
    protected final Class<?> entity;

    /**
     * Creates a new generator for the given entity class.
     */
    protected AccessorMethods(Class<?> entity) {
        this.entity = entity;
    }

    /**
     * Emits the generated or augmented class bytecode.
     */
    public abstract byte[] emit();

    /**
     * Reads the class file bytes for the given entity.
     */
    protected ClassModel readClassFiltering() {
        ClassModel model = GenerationUtils.readClassModel(entity);
        if (model == null) {
            throw new MappingException("Could not find class file for %s".formatted(entity.getName()));
        }
        return model;
    }
}
