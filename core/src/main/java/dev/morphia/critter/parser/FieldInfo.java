package dev.morphia.critter.parser;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

import io.smallrye.classfile.Annotation;

/**
 * Immutable record capturing the bytecode-level information for a single entity field,
 * replacing direct use of ASM {@code FieldNode}.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public record FieldInfo(
        String name,
        String desc,
        String signature,
        int access,
        List<Annotation> visibleAnnotations,
        Class<?> declaringClass) {

    public FieldInfo(String name, String desc, String signature, int access, List<Annotation> visibleAnnotations) {
        this(name, desc, signature, access, visibleAnnotations, null);
    }
}
