package dev.morphia.critter.parser;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention;

import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.ARRAY;

/**
 * Provides shared Morphia configuration, mapper, and ASM type conversion utilities for Critter generators.
 */
public class Generators {
    private final MorphiaConfig config;
    private final Mapper mapper;
    /** The default Morphia naming convention applied during code generation. */
    public final MorphiaDefaultsConvention convention = new MorphiaDefaultsConvention();

    /**
     * Creates a new Generators instance with the given configuration and mapper.
     *
     * @param config the Morphia configuration
     * @param mapper the Morphia mapper
     */
    public Generators(MorphiaConfig config, Mapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    /**
     * Returns the {@link MorphiaConfig}.
     *
     * @return the Morphia configuration
     */
    public MorphiaConfig getConfig() {
        return config;
    }

    /**
     * Returns the {@link Mapper}.
     *
     * @return the Morphia mapper
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Wraps a primitive ASM {@link Type} with its corresponding boxed wrapper type.
     * Non-primitive types are returned unchanged.
     *
     * @param fieldType the ASM type to wrap
     * @return the boxed wrapper type, or the original type if not primitive
     */
    public static Type wrap(Type fieldType) {
        if (fieldType.equals(Type.VOID_TYPE))
            return Type.getType(Void.class);
        if (fieldType.equals(Type.BOOLEAN_TYPE))
            return Type.getType(Boolean.class);
        if (fieldType.equals(Type.CHAR_TYPE))
            return Type.getType(Character.class);
        if (fieldType.equals(Type.BYTE_TYPE))
            return Type.getType(Byte.class);
        if (fieldType.equals(Type.SHORT_TYPE))
            return Type.getType(Short.class);
        if (fieldType.equals(Type.INT_TYPE))
            return Type.getType(Integer.class);
        if (fieldType.equals(Type.FLOAT_TYPE))
            return Type.getType(Float.class);
        if (fieldType.equals(Type.LONG_TYPE))
            return Type.getType(Long.class);
        if (fieldType.equals(Type.DOUBLE_TYPE))
            return Type.getType(Double.class);
        return fieldType;
    }

    /**
     * Returns {@code true} if the given ASM type represents an array type.
     *
     * @param type the ASM type to check
     * @return {@code true} if the type is an array
     */
    public static boolean isArray(Type type) {
        return type.getSort() == ARRAY;
    }

    /**
     * Resolves an ASM {@link Type} to a {@link Class} using the current thread's context class loader.
     *
     * @param type the ASM type to resolve
     * @return the corresponding Java class
     */
    public static Class<?> asClass(Type type) {
        return asClass(type, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Resolves an ASM {@link Type} to a {@link Class} using the given class loader.
     *
     * @param type        the ASM type to resolve
     * @param classLoader the class loader used to locate the class
     * @return the corresponding Java class
     * @throws RuntimeException if the class cannot be found
     */
    public static Class<?> asClass(Type type, ClassLoader classLoader) {
        if (type.equals(Type.VOID_TYPE))
            return void.class;
        if (type.equals(Type.BOOLEAN_TYPE))
            return boolean.class;
        if (type.equals(Type.CHAR_TYPE))
            return char.class;
        if (type.equals(Type.BYTE_TYPE))
            return byte.class;
        if (type.equals(Type.SHORT_TYPE))
            return short.class;
        if (type.equals(Type.INT_TYPE))
            return int.class;
        if (type.equals(Type.FLOAT_TYPE))
            return float.class;
        if (type.equals(Type.LONG_TYPE))
            return long.class;
        if (type.equals(Type.DOUBLE_TYPE))
            return double.class;
        String className = type.getSort() == ARRAY
                ? type.getDescriptor().replace('/', '.')
                : type.getClassName();
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find class: %s".formatted(className), e);
        }
    }
}
