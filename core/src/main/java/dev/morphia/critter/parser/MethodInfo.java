package dev.morphia.critter.parser;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import io.smallrye.classfile.Annotation;

/**
 * Immutable record capturing the bytecode-level information for a single entity method,
 * replacing direct use of ASM {@code MethodNode}.
 *
 * @param declaringClass the class declaring the method, or null if unknown
 * @param setter         for a getter, the setter it pairs with, or null if unknown
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public record MethodInfo(
        String name,
        String desc,
        String signature,
        int access,
        List<Annotation> visibleAnnotations,
        @Nullable Class<?> declaringClass,
        @Nullable MethodInfo setter) {

    /**
     * Creates a MethodInfo whose declaring class isn't known.
     */
    public MethodInfo(String name, String desc, String signature, int access, List<Annotation> visibleAnnotations) {
        this(name, desc, signature, access, visibleAnnotations, null, null);
    }

    /**
     * Creates a MethodInfo with no setter.
     */
    public MethodInfo(String name, String desc, String signature, int access, List<Annotation> visibleAnnotations,
            @Nullable Class<?> declaringClass) {
        this(name, desc, signature, access, visibleAnnotations, declaringClass, null);
    }

    /**
     * Returns a new MethodInfo for this getter paired with {@code setter}, carrying the setter's annotations too.
     */
    public MethodInfo withSetter(MethodInfo setter) {
        java.util.List<Annotation> combined = new java.util.ArrayList<>();
        if (this.visibleAnnotations != null) {
            combined.addAll(this.visibleAnnotations);
        }
        if (setter.visibleAnnotations() != null) {
            combined.addAll(setter.visibleAnnotations());
        }
        return new MethodInfo(this.name, this.desc, this.signature, this.access, combined, this.declaringClass, setter);
    }
}
