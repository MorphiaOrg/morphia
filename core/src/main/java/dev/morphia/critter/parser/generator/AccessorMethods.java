package dev.morphia.critter.parser.generator;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.ClassTransform;

/**
 * Base class for bytecode generators that read and transform existing class files.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public abstract class AccessorMethods {
    /** The entity class whose bytecode will be augmented. */
    protected final Class<?> entity;
    /** The entity's parsed classfile. */
    protected final ClassModel model;

    /**
     * Creates a new generator for the given entity class, reading its classfile.
     */
    protected AccessorMethods(Class<?> entity) {
        this(entity, readClassModel(entity));
    }

    /**
     * Creates a new generator for the given entity class and its already parsed classfile.
     */
    protected AccessorMethods(Class<?> entity, ClassModel model) {
        this.entity = entity;
        this.model = model;
    }

    /**
     * Emits the generated or augmented class bytecode.
     */
    public abstract byte[] emit();

    /**
     * Weaves the entity: applies {@code transform}, adds the {@code __newN} methods instantiators call, and drops the build
     * stamp an earlier build left, for the generator to stamp it afresh (see {@link BuildStamp}).
     *
     * @param transform adds the accessor methods
     * @return the woven class's bytecode
     */
    protected byte[] weave(ClassTransform transform) {
        return ClassFile.of().transformClass(model,
                transform.andThen(InstantiatorGenerator.weaving(model)).andThen(BuildStamp.dropping()));
    }

    private static ClassModel readClassModel(Class<?> entity) {
        ClassModel model = GenerationUtils.readClassModel(entity);
        if (model == null) {
            throw new MappingException("Could not find class file for %s".formatted(entity.getName()));
        }
        return model;
    }
}
