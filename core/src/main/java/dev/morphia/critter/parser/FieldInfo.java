package dev.morphia.critter.parser;

import java.util.List;

import io.github.dmlloyd.classfile.Annotation;

/**
 * Immutable record capturing the bytecode-level information for a single entity field,
 * replacing direct use of ASM {@code FieldNode}.
 */
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
