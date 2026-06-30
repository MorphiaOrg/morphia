package dev.morphia.critter.parser.generator;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;

/**
 * Base class for ClassFile-based code generators that produce Critter accessor and model classes.
 */
public abstract class BaseGenerator {
    /** The entity class for which code is being generated. */
    protected final Class<?> entity;
    /** The class loader used to register generated class bytecode. */
    protected final CritterClassLoader critterClassLoader;
    /** The fully-qualified name of the class being generated. */
    protected String generatedType;
    /** The base package name derived from the entity, used to namespace generated types. */
    protected final String baseName;

    protected BaseGenerator(Class<?> entity, CritterClassLoader critterClassLoader) {
        this.entity = entity;
        this.critterClassLoader = critterClassLoader;
        this.baseName = Critter.critterPackage(entity);
    }
}
