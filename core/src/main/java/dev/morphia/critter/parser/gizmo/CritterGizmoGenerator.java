package dev.morphia.critter.parser.gizmo;

import java.io.IOException;
import java.util.List;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.Generators;
import dev.morphia.critter.parser.PropertyFinder;
import dev.morphia.critter.parser.asm.AddFieldAccessorMethods;
import dev.morphia.critter.parser.asm.AddMethodAccessorMethods;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class CritterGizmoGenerator {
    private CritterGizmoGenerator() {
    }

    public static GizmoEntityModelGenerator generate(Class<?> type, CritterClassLoader critterClassLoader,
            Generators generators, boolean runtimeMode) {
        ClassNode classNode = new ClassNode();
        String resourceName = "%s.class".formatted(type.getName().replace('.', '/'));
        java.io.InputStream inputStream = type.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find class file for %s".formatted(type.getName()));
        }
        try {
            new ClassReader(inputStream).accept(classNode, 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class %s".formatted(type.getName()), e);
        }
        PropertyFinder propertyFinder = new PropertyFinder(generators.getMapper(), critterClassLoader, runtimeMode);

        return entityModel(type, critterClassLoader, classNode, propertyFinder.find(type, classNode, generators.getConfig()),
                generators.getConfig());
    }

    public static GizmoEntityModelGenerator generate(Class<?> type, CritterClassLoader critterClassLoader,
            Generators generators) {
        return generate(type, critterClassLoader, generators, false);
    }

    public static byte[] fieldAccessors(Class<?> entityType, List<FieldNode> fields) {
        return new AddFieldAccessorMethods(entityType, fields).emit();
    }

    public static byte[] methodAccessors(Class<?> entityType, List<MethodNode> methods) {
        return new AddMethodAccessorMethods(entityType, methods).emit();
    }

    public static PropertyAccessorGenerator propertyAccessor(Class<?> entityType, CritterClassLoader critterClassLoader,
            FieldNode field) {
        return new PropertyAccessorGenerator(entityType, critterClassLoader, field).emit();
    }

    public static PropertyAccessorGenerator propertyAccessor(Class<?> entityType, CritterClassLoader critterClassLoader,
            MethodNode method) {
        return new PropertyAccessorGenerator(entityType, critterClassLoader, method).emit();
    }

    public static VarHandleAccessorGenerator varHandleAccessor(Class<?> entityType, CritterClassLoader critterClassLoader,
            FieldNode field) {
        return new VarHandleAccessorGenerator(entityType, critterClassLoader, field).emit();
    }

    public static VarHandleAccessorGenerator varHandleAccessor(Class<?> entityType, CritterClassLoader critterClassLoader,
            MethodNode method) {
        return new VarHandleAccessorGenerator(entityType, critterClassLoader, method).emit();
    }

    public static PropertyModelGenerator propertyModelGenerator(MorphiaConfig config, Class<?> entityType,
            CritterClassLoader critterClassLoader, FieldNode field) {
        return new PropertyModelGenerator(config, entityType, critterClassLoader, field).emit();
    }

    public static PropertyModelGenerator propertyModelGenerator(MorphiaConfig config, Class<?> entityType,
            CritterClassLoader critterClassLoader, MethodNode method) {
        return new PropertyModelGenerator(config, entityType, critterClassLoader, method).emit();
    }

    public static GizmoEntityModelGenerator entityModel(Class<?> type, CritterClassLoader critterClassLoader,
            ClassNode classNode, List<PropertyModelGenerator> properties, MorphiaConfig config) {
        return new GizmoEntityModelGenerator(type, critterClassLoader, classNode, properties, config).emit();
    }
}
