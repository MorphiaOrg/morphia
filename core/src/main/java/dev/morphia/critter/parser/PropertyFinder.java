package dev.morphia.critter.parser;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.generator.CritterGenerator;
import dev.morphia.critter.parser.generator.GenerationUtils;
import dev.morphia.critter.parser.generator.PropertyModelGenerator;
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
    private final List<String> annotationDescriptorKeys;
    private final CritterClassLoader classLoader;
    private final boolean runtimeMode;
    private final CritterGenerator critterGenerator;
    private final PropertyDiscovery propertyDiscovery;

    public PropertyFinder(Mapper mapper, CritterClassLoader classLoader, boolean runtimeMode) {
        this.providerMap = new LinkedHashMap<>();
        for (var provider : mapper.getConfig().propertyAnnotationProviders()) {
            providerMap.put(provider.provides(), provider);
        }
        this.annotationDescriptorKeys = providerMap.keySet().stream()
                .map(type -> "L" + type.getName().replace('.', '/') + ";")
                .toList();
        this.classLoader = classLoader;
        this.runtimeMode = runtimeMode;
        this.critterGenerator = new CritterGenerator(mapper);
        this.propertyDiscovery = mapper.getConfig().propertyDiscovery();
    }

    public List<PropertyModelGenerator> find(Class<?> entityType, ClassModel classModel) {
        return find(entityType, classModel, entityType);
    }

    /**
     * Discovers properties from {@code standinType}'s classfile but generates accessor and model
     * bytecode targeting {@code targetType}. Used for {@code @ExternalEntity} stand-ins where the
     * stand-in carries Morphia annotations but the target is the type actually persisted.
     */
    public List<PropertyModelGenerator> find(Class<?> standinType, ClassModel classModel, Class<?> targetType) {
        List<PropertyModelGenerator> models = new ArrayList<>();
        List<MethodInfo> methods = discoverPropertyMethods(standinType, classModel);
        if (methods.isEmpty()) {
            List<FieldInfo> fields = discoverAllFields(standinType, classModel);
            if (!runtimeMode) {
                checkAotCompatibility(fields, standinType, targetType, classModel);
                weaveFieldAccessors(standinType, targetType, fields);
            }
            for (FieldInfo field : fields) {
                if (runtimeMode) {
                    critterGenerator.nestmateAccessor(targetType, field);
                } else {
                    critterGenerator.propertyAccessor(targetType, classLoader, field);
                }
                models.add(critterGenerator.propertyModelGenerator(targetType, standinType, classLoader, field, runtimeMode));
            }
        } else {
            if (!runtimeMode) {
                checkAotMethodCompatibility(methods, standinType, targetType, classModel);
                classLoader.register(targetType.getName(), critterGenerator.methodAccessors(targetType, methods));
            }
            for (MethodInfo method : methods) {
                if (runtimeMode) {
                    critterGenerator.nestmateAccessor(targetType, method);
                } else {
                    critterGenerator.propertyAccessor(targetType, classLoader, method);
                }
                models.add(critterGenerator.propertyModelGenerator(targetType, standinType, classLoader, method, runtimeMode));
            }
        }
        return models;
    }

    private static final String ID_ANNOTATION_DESC = "Ldev/morphia/annotations/Id;";

    private void checkAotCompatibility(List<FieldInfo> fields, Class<?> standinType, Class<?> targetType,
            ClassModel classModel) {
        boolean hasIdOnField = false;
        for (FieldInfo field : fields) {
            int flags = field.access();
            if (standinType != targetType) {
                // Stand-ins keep the original rule: every accessor is woven into the target.
                if ((flags & ClassFile.ACC_PRIVATE) != 0 && field.declaringClass() != targetType) {
                    throw new UnsupportedOperationException(
                            "AOT skip: private inherited field '" + field.name() + "' from "
                                    + field.declaringClass().getName() + " in " + targetType.getName());
                }
            } else if (accessorOwner(field, standinType, targetType) != field.declaringClass()
                    && !isAccessibleFrom(field, targetType)) {
                throw new UnsupportedOperationException(
                        "AOT skip: inaccessible inherited field '" + field.name() + "' from "
                                + field.declaringClass().getName() + " in " + targetType.getName());
            }
            if (field.desc().startsWith("[")) {
                throw new UnsupportedOperationException(
                        "AOT skip: array-typed field '" + field.name() + "' in " + targetType.getName());
            }
            if (field.visibleAnnotations() != null && field.visibleAnnotations().stream()
                    .anyMatch(a -> ID_ANNOTATION_DESC.equals(a.classSymbol().descriptorString()))) {
                hasIdOnField = true;
            }
        }
        if (standinType == targetType) {
            checkShadowedFields(targetType, classModel);
        }
        // An entity without an @Id (e.g., an embedded type) is fine to generate from its fields. Only an @Id on a
        // getter needs the runtime, which can pick the discovery mode that finds it.
        if (!hasIdOnField) {
            checkIdOnGetter(standinType, targetType, classModel);
        }
    }

    /**
     * Adds the {@code __readXxx}/{@code __writeXxx} accessor methods for {@code fields}. Each field's accessors are woven into
     * the class declaring it when that class can be rewritten alongside the entity, so inherited private and package-private
     * fields are reachable; the entity inherits those methods. Fields from other superclasses (e.g. from a library) are woven
     * into the entity itself.
     * <p>
     * What is woven into a class depends only on that class, so subclasses sharing a superclass all produce the same bytes for
     * it.
     */
    private void weaveFieldAccessors(Class<?> standinType, Class<?> targetType, List<FieldInfo> fields) {
        if (standinType != targetType) {
            classLoader.register(targetType.getName(), critterGenerator.fieldAccessors(targetType, fields));
            return;
        }
        Set<Class<?>> owners = new LinkedHashSet<>();
        owners.add(targetType);
        for (FieldInfo field : fields) {
            owners.add(accessorOwner(field, standinType, targetType));
        }
        for (Class<?> owner : owners) {
            List<FieldInfo> ownerFields = owner == targetType ? fields : discoverAllFields(owner, null);
            List<FieldInfo> woven = ownerFields.stream()
                    .filter(field -> accessorOwner(field, owner, owner) == owner)
                    .toList();
            classLoader.register(owner.getName(), critterGenerator.fieldAccessors(owner, woven));
        }
    }

    /**
     * @return the class whose bytecode receives the accessor methods for {@code field}
     */
    private static Class<?> accessorOwner(FieldInfo field, Class<?> standinType, Class<?> targetType) {
        Class<?> declaring = field.declaringClass();
        return standinType == targetType && canWeave(declaring, targetType) ? declaring : targetType;
    }

    /**
     * A superclass can be rewritten alongside the entity when both were loaded from the same location, i.e. the same
     * output directory or jar. A superclass from a library must be left alone.
     */
    private static boolean canWeave(Class<?> type, Class<?> entity) {
        if (type == entity) {
            return true;
        }
        URL location = location(type);
        return location != null && location.equals(location(entity));
    }

    private static URL location(Class<?> type) {
        CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        return codeSource != null ? codeSource.getLocation() : null;
    }

    private static boolean isAccessibleFrom(FieldInfo field, Class<?> owner) {
        int flags = field.access();
        if ((flags & (ClassFile.ACC_PUBLIC | ClassFile.ACC_PROTECTED)) != 0) {
            return true;
        }
        return (flags & ClassFile.ACC_PRIVATE) == 0
                && field.declaringClass().getPackageName().equals(owner.getPackageName())
                && field.declaringClass().getClassLoader() == owner.getClassLoader();
    }

    /**
     * Rejects an entity that redeclares a field from a superclass that is woven too: both classes would get the same
     * {@code __readXxx}/{@code __writeXxx} methods, and the subclass's would override the superclass's.
     */
    private void checkShadowedFields(Class<?> targetType, ClassModel classModel) {
        Map<String, Class<?>> declaredBy = new LinkedHashMap<>();
        Class<?> current = targetType;
        ClassModel currentModel = classModel;
        while (current != null && current != Object.class && canWeave(current, targetType)) {
            ClassModel model = currentModel != null ? currentModel : readClassModel(current);
            if (model == null) {
                break;
            }
            for (FieldInfo field : discoverFields(model, current)) {
                Class<?> shadowing = declaredBy.putIfAbsent(field.name(), current);
                if (shadowing != null) {
                    throw new UnsupportedOperationException(
                            "AOT skip: field '" + field.name() + "' in " + shadowing.getName() + " shadows a field in "
                                    + current.getName());
                }
            }
            current = current.getSuperclass();
            currentModel = null;
        }
    }

    private void checkAotMethodCompatibility(List<MethodInfo> methods, Class<?> standinType, Class<?> targetType,
            ClassModel classModel) {
        // If @Id is missing from the discovered methods, look for it on any getter in the hierarchy.
        boolean hasIdInMethods = methods.stream()
                .anyMatch(m -> m.visibleAnnotations() != null && m.visibleAnnotations().stream()
                        .anyMatch(a -> ID_ANNOTATION_DESC.equals(a.classSymbol().descriptorString())));
        if (hasIdInMethods) {
            return;
        }
        checkIdOnGetter(standinType, targetType, classModel);
    }

    /**
     * Skips AOT when {@code @Id} is on a getter anywhere in the hierarchy: the entity relies on METHODS discovery for
     * its id, so the runtime must pick the discovery mode. The scan walks {@code standinType}'s hierarchy (the type
     * {@code classModel} describes), which differs from {@code targetType} for {@code @ExternalEntity} stand-ins.
     */
    private void checkIdOnGetter(Class<?> standinType, Class<?> targetType, ClassModel classModel) {
        ClassModel current = classModel;
        Class<?> cls = standinType;
        while (cls != null && cls != Object.class) {
            ClassModel model = current != null ? current : readClassModel(cls);
            if (model != null) {
                for (io.github.dmlloyd.classfile.MethodModel method : model.methods()) {
                    if (visibleAnnotations(method).stream()
                            .anyMatch(a -> ID_ANNOTATION_DESC.equals(a.classSymbol().descriptorString()))) {
                        throw new UnsupportedOperationException(
                                "AOT skip: @Id on getter in " + targetType.getName()
                                        + "; entity requires runtime property discovery");
                    }
                }
            }
            cls = cls.getSuperclass();
            current = null;
        }
    }

    private boolean isPropertyAnnotated(List<Annotation> annotations, boolean allowUnannotated) {
        List<Annotation> anns = annotations != null ? annotations : List.of();
        return allowUnannotated || anns.stream()
                .anyMatch(a -> annotationDescriptorKeys.contains(a.classSymbol().descriptorString()));
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
            final Class<?> declaringClass = current;
            for (FieldInfo field : discoverFields(model, declaringClass)) {
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
        try {
            ClassModel model = GenerationUtils.readClassModel(type);
            if (model == null) {
                LOG.debug("Bytecode resource not found for {}; hierarchy traversal stops here", type.getName());
            }
            return model;
        } catch (RuntimeException e) {
            LOG.warn("Failed to read bytecode for {}; hierarchy traversal stops here: {}", type.getName(), e.getMessage());
            return null;
        }
    }

    private List<FieldInfo> discoverFields(ClassModel classModel, Class<?> declaringClass) {
        List<String> transientDescs = CritterParser.INSTANCE.transientAnnotations();
        List<FieldInfo> result = new ArrayList<>();
        for (FieldModel field : classModel.fields()) {
            List<Annotation> visible = visibleAnnotations(field);
            int flags = field.flags().flagsMask();
            boolean isTransient = (flags & ClassFile.ACC_TRANSIENT) != 0
                    || visible.stream().map(a -> a.classSymbol().descriptorString()).anyMatch(transientDescs::contains);
            boolean isStatic = (flags & ClassFile.ACC_STATIC) != 0;
            if (!isTransient && !isStatic && isPropertyAnnotated(visible, true)) {
                String sig = field.findAttribute(signature())
                        .map(a -> a.signature().stringValue())
                        .orElse(null);
                result.add(new FieldInfo(
                        field.fieldName().stringValue(),
                        field.fieldType().stringValue(),
                        sig,
                        field.flags().flagsMask(),
                        visible,
                        declaringClass));
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
        if (name.equals("get") || name.equals("is"))
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
                    && method.methodType().stringValue().equals(setterDesc)
                    && (method.flags().flagsMask() & ClassFile.ACC_STATIC) == 0) {
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
                if (setter != null && (setter.access() & ClassFile.ACC_PRIVATE) == 0
                        && (setter.access() & ClassFile.ACC_STATIC) == 0) {
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
