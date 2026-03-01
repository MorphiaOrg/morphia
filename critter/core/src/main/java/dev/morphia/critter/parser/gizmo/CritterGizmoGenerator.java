package dev.morphia.critter.parser.gizmo;

import java.io.IOException;
import java.util.List;

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
    public static final CritterGizmoGenerator INSTANCE = new CritterGizmoGenerator();

    private CritterGizmoGenerator() {
    }

    public GizmoEntityModelGenerator generate(Class<?> type, CritterClassLoader critterClassLoader, boolean runtimeMode) {
        ClassNode classNode = new ClassNode();
        String resourceName = type.getName().replace('.', '/') + ".class";
        java.io.InputStream inputStream = type.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find class file for " + type.getName());
        }
        try {
            new ClassReader(inputStream).accept(classNode, 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class " + type.getName(), e);
        }
        PropertyFinder propertyFinder = new PropertyFinder(Generators.INSTANCE.getMapper(), critterClassLoader, runtimeMode);

        return entityModel(type, critterClassLoader, classNode, propertyFinder.find(type, classNode));
    }

    public GizmoEntityModelGenerator generate(Class<?> type, CritterClassLoader critterClassLoader) {
        return generate(type, critterClassLoader, false);
    }

    public byte[] fieldAccessors(Class<?> entityType, List<FieldNode> fields) {
        return new AddFieldAccessorMethods(entityType, fields).emit();
    }

    public byte[] methodAccessors(Class<?> entityType, List<MethodNode> methods) {
        return new AddMethodAccessorMethods(entityType, methods).emit();
    }

    public PropertyAccessorGenerator propertyAccessor(Class<?> entityType, CritterClassLoader critterClassLoader, FieldNode field) {
        return new PropertyAccessorGenerator(entityType, critterClassLoader, field).emit();
    }

    public PropertyAccessorGenerator propertyAccessor(Class<?> entityType, CritterClassLoader critterClassLoader, MethodNode method) {
        return new PropertyAccessorGenerator(entityType, critterClassLoader, method).emit();
    }

    public VarHandleAccessorGenerator varHandleAccessor(Class<?> entityType, CritterClassLoader critterClassLoader, FieldNode field) {
        return new VarHandleAccessorGenerator(entityType, critterClassLoader, field).emit();
    }

    public VarHandleAccessorGenerator varHandleAccessor(Class<?> entityType, CritterClassLoader critterClassLoader, MethodNode method) {
        return new VarHandleAccessorGenerator(entityType, critterClassLoader, method).emit();
    }

    public PropertyModelGenerator propertyModelGenerator(Class<?> entityType, CritterClassLoader critterClassLoader, FieldNode field) {
        return new PropertyModelGenerator(Generators.INSTANCE.getConfig(), entityType, critterClassLoader, field).emit();
    }

    public PropertyModelGenerator propertyModelGenerator(Class<?> entityType, CritterClassLoader critterClassLoader, MethodNode method) {
        return new PropertyModelGenerator(Generators.INSTANCE.getConfig(), entityType, critterClassLoader, method).emit();
    }

    public GizmoEntityModelGenerator entityModel(Class<?> type, CritterClassLoader critterClassLoader,
            ClassNode classNode, List<PropertyModelGenerator> properties) {
        return new GizmoEntityModelGenerator(type, critterClassLoader, classNode, properties).emit();
    }
}
