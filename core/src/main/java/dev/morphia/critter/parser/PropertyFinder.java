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
import dev.morphia.mapping.PropertyDiscovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dmlloyd.classfile.Annotation;
import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.FieldModel;
import io.github.dmlloyd.classfile.MethodModel;
import io.github.dmlloyd.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

import static io.github.dmlloyd.classfile.Attributes.runtimeVisibleAnnotations;
import static io.github.dmlloyd.classfile.Attributes.signature;

/**
 * Discovers entity properties (fields or getter methods) from a parsed class model
 * and produces the corresponding {@link PropertyModelGenerator} instances.
 */
public class PropertyFinder {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyFinder.class);

    private final Map<Class<?>, Object> providerMap;
    private final CritterClassLoader classLoader;
    private final boolean runtimeMode;
    private final CritterGizmoGenerator critterGizmoGenerator;
    private final PropertyDiscovery propertyDiscovery;

    public PropertyFinder(Mapper mapper, CritterClassLoader classLoader, boolean runtimeMode) {
        this.providerMap = new LinkedHashMap<>();
        for (var provider : mapper.getConfig().propertyAnnotationProviders()) {
            providerMap.put(provider.provides(), provider);
        }
        this.classLoader = classLoader;
        this.runtimeMode = runtimeMode;
        this.critterGizmoGenerator = new CritterGizmoGenerator(mapper);
        this.propertyDiscovery = mapper.getConfig().propertyDiscovery();
    }

    public List<PropertyModelGenerator> find(Class<?> entityType, ClassModel classModel) {
        List<PropertyModelGenerator> models = new ArrayList<>();
        List<MethodInfo> methods = discoverPropertyMethods(entityType, classModel);
        if (methods.isEmpty()) {
            List<FieldInfo> fields = discoverAllFields(entityType, classModel);
            if (!runtimeMode) {
                classLoader.register(entityType.getName(), critterGizmoGenerator.fieldAccessors(entityType, fields));
            }
            for (FieldInfo field : fields) {
                if (runtimeMode) {
                    critterGizmoGenerator.varHandleAccessor(entityType, classLoader, field);
                } else {
                    critterGizmoGenerator.propertyAccessor(entityType, classLoader, field);
                }
                models.add(critterGizmoGenerator.propertyModelGenerator(entityType, classLoader, field));
            }
        } else {
            if (!runtimeMode) {
                classLoader.register(entityType.getName(), critterGizmoGenerator.methodAccessors(entityType, methods));
            }
            for (MethodInfo method : methods) {
                if (runtimeMode) {
                    critterGizmoGenerator.varHandleAccessor(entityType, classLoader, method);
                } else {
                    critterGizmoGenerator.propertyAccessor(entityType, classLoader, method);
                }
                models.add(critterGizmoGenerator.propertyModelGenerator(entityType, classLoader, method));
            }
        }
        return models;
    }

    private boolean isPropertyAnnotated(List<Annotation> annotations, boolean allowUnannotated) {
        List<Annotation> anns = annotations != null ? annotations : List.of();
        List<String> keys = providerMap.keySet().stream()
                .map(type -> "L" + type.getName().replace('.', '/') + ";")
                .toList();
        return allowUnannotated || anns.stream()
                .anyMatch(a -> keys.contains(a.classSymbol().descriptorString()));
    }

    private List<FieldInfo> discoverAllFields(Class<?> entityType, ClassModel classModel) {
        List<FieldInfo> fields = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        Class<?> current = entityType;
        ClassModel currentModel = classModel;

        while (current != null && current != Object.class) {
            ClassModel model = currentModel != null ? currentModel : readClassModel(current);
            if (model == null)
                break;
            for (FieldInfo field : discoverFields(model)) {
                if (seen.putIfAbsent(field.name(), Boolean.TRUE) == null) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
            currentModel = null;
        }
        return fields;
    }

    private ClassModel readClassModel(Class<?> type) {
        String resourceName = "%s.class".formatted(type.getName().replace('.', '/'));
        ClassLoader cl = type.getClassLoader() != null ? type.getClassLoader()
                : ClassLoader.getSystemClassLoader();
        InputStream inputStream = cl.getResourceAsStream(resourceName);
        if (inputStream == null) {
            LOG.debug("Bytecode resource not found for {}; hierarchy traversal stops here", type.getName());
            return null;
        }
        try {
            byte[] bytes = inputStream.readAllBytes();
            return ClassFile.of().parse(bytes);
        } catch (IOException e) {
            LOG.warn("Failed to read bytecode for {}: {}", type.getName(), e.getMessage());
            return null;
        }
    }

    private List<FieldInfo> discoverFields(ClassModel classModel) {
        List<String> transientDescs = CritterParser.INSTANCE.transientAnnotations();
        List<FieldInfo> result = new ArrayList<>();
        for (FieldModel field : classModel.fields()) {
            List<Annotation> visible = visibleAnnotations(field);
            boolean isTransient = (field.flags().flagsMask() & ClassFile.ACC_TRANSIENT) != 0
                    || visible.stream().map(a -> a.classSymbol().descriptorString()).anyMatch(transientDescs::contains);
            if (!isTransient && isPropertyAnnotated(visible, true)) {
                String sig = field.findAttribute(signature())
                        .map(a -> a.signature().stringValue())
                        .orElse(null);
                result.add(new FieldInfo(
                        field.fieldName().stringValue(),
                        field.fieldType().stringValue(),
                        sig,
                        field.flags().flagsMask(),
                        visible));
            }
        }
        return result;
    }

    private List<MethodInfo> discoverPropertyMethods(Class<?> entityType, ClassModel classModel) {
        List<MethodInfo> result = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();

        Class<?> current = entityType;
        ClassModel currentModel = classModel;

        while (current != null && current != Object.class) {
            ClassModel model = currentModel != null ? currentModel : readClassModel(current);
            if (model == null)
                break;

            boolean isSuperclass = current != entityType;
            for (MethodModel method : model.methods()) {
                if (!isGetter(method))
                    continue;
                if (isSuperclass && (method.flags().flagsMask() & ClassFile.ACC_PRIVATE) != 0)
                    continue;
                String propName = getterPropertyName(method);
                if (seen.containsKey(propName))
                    continue;

                MethodInfo methodInfo = toMethodInfo(method);

                if (propertyDiscovery == PropertyDiscovery.METHODS) {
                    java.lang.constant.MethodTypeDesc mtd = java.lang.constant.MethodTypeDesc
                            .ofDescriptor(method.methodType().stringValue());
                    MethodInfo setter = findSetterInHierarchy(model, current, propName,
                            mtd.returnType().descriptorString());
                    if (setter != null) {
                        seen.put(propName, Boolean.TRUE);
                        result.add(methodInfo.mergeAnnotations(setter));
                    }
                } else if (isPropertyAnnotated(methodInfo.visibleAnnotations(), false)) {
                    seen.put(propName, Boolean.TRUE);
                    result.add(methodInfo);
                }
            }

            current = current.getSuperclass();
            currentModel = null;
        }
        return result;
    }

    private MethodInfo toMethodInfo(MethodModel method) {
        String sig = method.findAttribute(signature())
                .map(a -> a.signature().stringValue())
                .orElse(null);
        return new MethodInfo(
                method.methodName().stringValue(),
                method.methodType().stringValue(),
                sig,
                method.flags().flagsMask(),
                visibleAnnotations(method));
    }

    private boolean isGetter(MethodModel method) {
        String name = method.methodName().stringValue();
        if (!name.startsWith("get") && !name.startsWith("is"))
            return false;
        int flags = method.flags().flagsMask();
        if ((flags & ClassFile.ACC_STATIC) != 0)
            return false;
        // 0x0040 = ACC_BRIDGE: skip compiler-generated covariant bridge methods
        if ((flags & 0x0040) != 0)
            return false;
        java.lang.constant.MethodTypeDesc mtd = java.lang.constant.MethodTypeDesc
                .ofDescriptor(method.methodType().stringValue());
        return mtd.parameterCount() == 0 && !mtd.returnType().equals(java.lang.constant.ConstantDescs.CD_void);
    }

    private String getterPropertyName(MethodModel method) {
        String name = method.methodName().stringValue();
        String prefix = name.startsWith("is") ? "is" : "get";
        String prop = name.substring(prefix.length());
        return Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
    }

    private MethodInfo findSetter(ClassModel classModel, String propertyName, String returnDesc) {
        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        String setterDesc = "(" + returnDesc + ")V";
        for (MethodModel method : classModel.methods()) {
            if (method.methodName().stringValue().equals(setterName)
                    && method.methodType().stringValue().equals(setterDesc)) {
                return toMethodInfo(method);
            }
        }
        return null;
    }

    private MethodInfo findSetterInHierarchy(ClassModel startModel, Class<?> startClass, String propName,
            String returnDesc) {
        ClassModel model = startModel;
        Class<?> current = startClass;
        while (current != null && current != Object.class) {
            if (model == null)
                model = readClassModel(current);
            if (model != null) {
                MethodInfo setter = findSetter(model, propName, returnDesc);
                if (setter != null && (setter.access() & ClassFile.ACC_PRIVATE) == 0) {
                    return setter;
                }
            }
            current = current.getSuperclass();
            model = null;
        }
        return null;
    }

    private List<Annotation> visibleAnnotations(io.github.dmlloyd.classfile.AttributedElement element) {
        return element.findAttribute(runtimeVisibleAnnotations())
                .map(RuntimeVisibleAnnotationsAttribute::annotations)
                .orElse(List.of());
    }
}
