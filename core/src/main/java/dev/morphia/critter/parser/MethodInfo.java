package dev.morphia.critter.parser;

import java.util.List;

import io.github.dmlloyd.classfile.Annotation;

/**
 * Immutable record capturing the bytecode-level information for a single entity method,
 * replacing direct use of ASM {@code MethodNode}.
 */
public record MethodInfo(
        String name,
        String desc,
        String signature,
        int access,
        List<Annotation> visibleAnnotations) {

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
        return new MethodInfo(this.name, this.desc, this.signature, this.access, combined);
    }
}
