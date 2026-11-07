package dev.morphia.critter.parser;

import java.util.List;

import com.mongodb.lang.Nullable;

import io.smallrye.classfile.Annotation;

/**
 * Immutable record capturing the bytecode-level information for a single entity method,
 * replacing direct use of ASM {@code MethodNode}.
 *
 * @param declaringClass the class declaring the method, or null if unknown
 */
public record MethodInfo(
        String name,
        String desc,
        String signature,
        int access,
        List<Annotation> visibleAnnotations,
        @Nullable Class<?> declaringClass) {

    /**
     * Creates a MethodInfo whose declaring class isn't known.
     */
    public MethodInfo(String name, String desc, String signature, int access, List<Annotation> visibleAnnotations) {
        this(name, desc, signature, access, visibleAnnotations, null);
    }

    /**
     * Returns a new MethodInfo that merges annotations from this (getter) and the setter.
     * If the setter has no annotations, returns {@code this}.
     */
    public MethodInfo mergeAnnotations(MethodInfo setter) {
        List<Annotation> setterAnnotations = setter.visibleAnnotations() != null
                ? setter.visibleAnnotations()
                : List.of();
        if (setterAnnotations.isEmpty()) {
            return this;
        }
        java.util.List<Annotation> combined = new java.util.ArrayList<>();
        if (this.visibleAnnotations != null) {
            combined.addAll(this.visibleAnnotations);
        }
        combined.addAll(setterAnnotations);
        return new MethodInfo(this.name, this.desc, this.signature, this.access, combined, this.declaringClass);
    }
}
