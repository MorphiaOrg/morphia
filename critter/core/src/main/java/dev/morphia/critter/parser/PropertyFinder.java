package dev.morphia.critter.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator;
import dev.morphia.critter.parser.gizmo.PropertyModelGenerator;
import dev.morphia.critter.parser.java.CritterParser;
import dev.morphia.mapping.Mapper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class PropertyFinder {
    private final Map<Class<?>, Object> providerMap;
    private final CritterClassLoader classLoader;
    private final boolean runtimeMode;

    public PropertyFinder(Mapper mapper, CritterClassLoader classLoader, boolean runtimeMode) {
        this.providerMap = new LinkedHashMap<>();
        for (var provider : mapper.getConfig().propertyAnnotationProviders()) {
            providerMap.put(provider.provides(), provider);
        }
        this.classLoader = classLoader;
        this.runtimeMode = runtimeMode;
    }

    public List<PropertyModelGenerator> find(Class<?> entityType, ClassNode classNode) {
        List<PropertyModelGenerator> models = new ArrayList<>();
        List<MethodNode> methods = discoverPropertyMethods(classNode);
        if (methods.isEmpty()) {
            List<FieldNode> fields = discoverAllFields(entityType, classNode);
            if (!runtimeMode) {
                classLoader.register(entityType.getName(), CritterGizmoGenerator.INSTANCE.fieldAccessors(entityType, fields));
            }
            for (FieldNode field : fields) {
                if (runtimeMode) {
                    CritterGizmoGenerator.INSTANCE.varHandleAccessor(entityType, classLoader, field);
                } else {
                    CritterGizmoGenerator.INSTANCE.propertyAccessor(entityType, classLoader, field);
                }
                models.add(CritterGizmoGenerator.INSTANCE.propertyModelGenerator(entityType, classLoader, field));
            }
        } else {
            if (!runtimeMode) {
                classLoader.register(entityType.getName(), CritterGizmoGenerator.INSTANCE.methodAccessors(entityType, methods));
            }
            for (MethodNode method : methods) {
                if (runtimeMode) {
                    CritterGizmoGenerator.INSTANCE.varHandleAccessor(entityType, classLoader, method);
                } else {
                    CritterGizmoGenerator.INSTANCE.propertyAccessor(entityType, classLoader, method);
                }
                models.add(CritterGizmoGenerator.INSTANCE.propertyModelGenerator(entityType, classLoader, method));
            }
        }
        return models;
    }

    private boolean isPropertyAnnotated(List<AnnotationNode> annotationNodes, boolean allowUnannotated) {
        List<AnnotationNode> annotations = annotationNodes != null ? annotationNodes : List.of();
        List<String> keys = providerMap.keySet().stream()
                .map(type -> Type.getType(type).getDescriptor())
                .toList();
        return allowUnannotated || annotations.stream().anyMatch(a -> keys.contains(a.desc));
    }

    private List<FieldNode> discoverAllFields(Class<?> entityType, ClassNode classNode) {
        List<FieldNode> fields = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        Class<?> current = entityType;
        ClassNode currentNode = classNode;

        while (current != null && current != Object.class) {
            ClassNode node = currentNode != null ? currentNode : readClassNode(current);
            if (node == null)
                break;
            for (FieldNode field : discoverFields(node)) {
                if (seen.putIfAbsent(field.name, Boolean.TRUE) == null) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
            currentNode = null;
        }
        return fields;
    }

    private ClassNode readClassNode(Class<?> type) {
        String resourceName = type.getName().replace('.', '/') + ".class";
        InputStream inputStream = type.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null)
            return null;
        ClassNode node = new ClassNode();
        try {
            new ClassReader(inputStream).accept(node, 0);
        } catch (IOException e) {
            return null;
        }
        return node;
    }

    private List<FieldNode> discoverFields(ClassNode classNode) {
        List<String> transientDescs = CritterParser.INSTANCE.transientAnnotations();
        List<FieldNode> result = new ArrayList<>();
        for (FieldNode field : classNode.fields) {
            List<AnnotationNode> visible = field.visibleAnnotations != null ? field.visibleAnnotations : List.of();
            boolean isTransient = visible.stream().map(a -> a.desc).anyMatch(transientDescs::contains);
            if (!isTransient && isPropertyAnnotated(field.visibleAnnotations, true)) {
                result.add(field);
            }
        }
        return result;
    }

    private List<MethodNode> discoverPropertyMethods(ClassNode classNode) {
        List<MethodNode> result = new ArrayList<>();
        for (MethodNode method : classNode.methods) {
            if (method.name.startsWith("get") && Type.getArgumentTypes(method.desc).length == 0) {
                if (isPropertyAnnotated(method.visibleAnnotations, false)) {
                    result.add(method);
                }
            }
        }
        return result;
    }
}
