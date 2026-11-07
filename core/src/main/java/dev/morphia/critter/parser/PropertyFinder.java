package dev.morphia.critter.parser;

import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.IdField;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.generator.CritterGenerator;
import dev.morphia.critter.parser.generator.GenerationUtils;
import dev.morphia.critter.parser.generator.PropertyModelGenerator;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.mapping.codec.pojo.ClassHierarchy;
import dev.morphia.mapping.conventions.DiscoveryRules;
import dev.morphia.mapping.conventions.DiscoveryRules.AccessorPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.FieldModel;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

import static io.smallrye.classfile.Attributes.runtimeVisibleAnnotations;
import static io.smallrye.classfile.Attributes.signature;

/**
 * Discovers entity properties (fields or getter methods) from a parsed class model
 * and produces the corresponding {@link PropertyModelGenerator} instances.
 */
public class PropertyFinder {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyFinder.class);

    private final CritterClassLoader classLoader;
    private final boolean runtimeMode;
    private final CritterGenerator critterGenerator;
    private final PropertyDiscovery propertyDiscovery;
    private final MorphiaConfig config;

    public PropertyFinder(Mapper mapper, CritterClassLoader classLoader, boolean runtimeMode) {
        this.classLoader = classLoader;
        this.runtimeMode = runtimeMode;
        this.critterGenerator = new CritterGenerator(mapper);
        this.propertyDiscovery = mapper.getConfig().propertyDiscovery();
        this.config = mapper.getConfig();
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
        if (propertyDiscovery == PropertyDiscovery.FIELDS) {
            List<FieldInfo> fields = discoverAllFields(standinType, classModel);
            checkIdField(standinType, targetType, fields.stream().map(FieldInfo::name).toList(),
                    fields.stream().anyMatch(f -> isAnnotatedId(f.visibleAnnotations())));
            if (!runtimeMode) {
                checkAotCompatibility(fields, standinType, targetType, classModel);
                weaveFieldAccessors(standinType, targetType, fields);
            }
            for (FieldInfo field : fields) {
                if (runtimeMode) {
                    critterGenerator.nestmateAccessor(targetType, classLoader, field);
                } else {
                    critterGenerator.propertyAccessor(targetType, classLoader, field);
                }
                models.add(new PropertyModelGenerator(config, targetType, standinType, classLoader, field, runtimeMode));
            }
        } else {
            List<MethodInfo> methods = discoverPropertyMethods(standinType, classModel, standinType != targetType);
            checkIdField(standinType, targetType, methods.stream().map(m -> DiscoveryRules.propertyName(m.name())).toList(),
                    methods.stream().anyMatch(m -> isAnnotatedId(m.visibleAnnotations())));
            if (!runtimeMode) {
                checkAotMethodCompatibility(methods, standinType, targetType, classModel);
                classLoader.register(targetType.getName(), critterGenerator.methodAccessors(targetType, methods));
            }
            for (MethodInfo method : methods) {
                if (runtimeMode) {
                    critterGenerator.nestmateAccessor(targetType, classLoader, method);
                } else {
                    critterGenerator.propertyAccessor(targetType, classLoader, method);
                }
                models.add(new PropertyModelGenerator(config, targetType, standinType, classLoader, method, runtimeMode));
            }
        }
        // as in ConfigureProperties, an explicit @Id wins over the entity's @IdField
        boolean explicitId = models.stream().anyMatch(PropertyModelGenerator::isAnnotatedId);
        if (explicitId) {
            models.forEach(PropertyModelGenerator::ignoreIdField);
        }
        return models;
    }

    private static final String ID_ANNOTATION_DESC = "Ldev/morphia/annotations/Id;";

    /**
     * As in {@code ConfigureProperties}, an {@code @IdField} that names no property fails the mapping unless a property
     * is annotated {@code @Id}. Checked before anything is generated, so a failed entity leaves nothing behind.
     */
    private static void checkIdField(Class<?> standinType, Class<?> targetType, List<String> properties, boolean explicitId) {
        IdField idField = (IdField) ClassHierarchy.annotations(standinType).get(IdField.class);
        if (!explicitId && idField != null && !properties.contains(idField.value())) {
            throw new MappingException("@IdField(\"%s\") on %s doesn't name a property".formatted(idField.value(),
                    targetType.getName()));
        }
    }

    private static boolean isAnnotatedId(List<Annotation> annotations) {
        return annotations != null && annotations.stream()
                .anyMatch(a -> ID_ANNOTATION_DESC.equals(a.classSymbol().descriptorString()));
    }

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
            if (field.visibleAnnotations() != null && field.visibleAnnotations().stream()
                    .anyMatch(a -> ID_ANNOTATION_DESC.equals(a.classSymbol().descriptorString()))) {
                hasIdOnField = true;
            }
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
                for (io.smallrye.classfile.MethodModel method : model.methods()) {
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

    private List<FieldInfo> discoverAllFields(Class<?> entityType, ClassModel classModel) {
        List<FieldInfo> fields = new ArrayList<>();
        Map<String, Class<?>> seen = new LinkedHashMap<>();
        Class<?> current = entityType;
        ClassModel currentModel = classModel;

        while (current != null && current != Object.class) {
            ClassModel model = currentModel != null ? currentModel : readClassModel(current);
            if (model == null)
                break;
            final Class<?> declaringClass = current;
            for (FieldInfo field : discoverFields(model, declaringClass)) {
                Class<?> shadowing = seen.putIfAbsent(field.name(), declaringClass);
                if (shadowing != null) {
                    // as for reflection, whose field discovery maps both fields to the same name
                    throw new MappingException("Field '%s' in %s shadows a field in %s".formatted(field.name(),
                            shadowing.getName(), declaringClass.getName()));
                }
                fields.add(field);
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
        List<FieldInfo> result = new ArrayList<>();
        for (FieldModel field : classModel.fields()) {
            List<Annotation> visible = visibleAnnotations(field);
            int flags = field.flags().flagsMask();
            if (!DiscoveryRules.isIgnoredField(flags, annotationNames(visible))) {
                String sig = field.findAttribute(signature())
                        .map(a -> a.signature().stringValue())
                        .orElse(null);
                result.add(new FieldInfo(
                        field.fieldName().stringValue(),
                        field.fieldType().stringValue(),
                        sig,
                        flags,
                        visible,
                        declaringClass));
            }
        }
        return result;
    }

    private static List<String> annotationNames(List<Annotation> annotations) {
        return annotations.stream()
                .map(a -> a.classSymbol().descriptorString())
                .map(desc -> desc.substring(1, desc.length() - 1).replace('/', '.'))
                .toList();
    }

    /**
     * Pairs getters and setters the way {@link dev.morphia.mapping.conventions.MethodDiscovery} does: within each class
     * of the hierarchy, with a subclass's pair hiding a superclass's for the same property.
     */
    private List<MethodInfo> discoverPropertyMethods(Class<?> entityType, ClassModel classModel, boolean standin) {
        Map<String, MethodInfo> properties = new LinkedHashMap<>();

        Class<?> current = entityType;
        ClassModel currentModel = classModel;

        while (current != null && current != Object.class) {
            ClassModel model = currentModel != null ? currentModel : readClassModel(current);
            if (model == null)
                break;

            for (AccessorPair<MethodModel> pair : DiscoveryRules.accessorPairs(model.methods(),
                    m -> m.methodName().stringValue(), m -> m.flags().flagsMask())) {
                if (!properties.containsKey(pair.property())) {
                    properties.put(pair.property(), toMethodInfo(pair, current, entityType, standin));
                }
            }

            current = current.getSuperclass();
            currentModel = null;
        }
        return new ArrayList<>(properties.values());
    }

    /**
     * @return the getter, paired with its setter and carrying the setter's annotations too
     * @throws UnsupportedOperationException if the generated accessors can't call the pair: they take the getter to be a
     *                                       {@code get}/{@code is} method with no parameters and the setter to be named for
     *                                       the property and take the getter's type, returning anything. Build-time accessors
     *                                       are woven into the entity, so they also can't call a superclass's private
     *                                       methods or those hidden from the entity's package: see
     *                                       {@link #isCallable}
     */
    private MethodInfo toMethodInfo(AccessorPair<MethodModel> pair, Class<?> declaringClass, Class<?> entityType,
            boolean standin) {
        MethodTypeDesc getter = MethodTypeDesc.ofDescriptor(pair.getter().methodType().stringValue());
        MethodTypeDesc setter = MethodTypeDesc.ofDescriptor(pair.setter().methodType().stringValue());
        String getterName = pair.getter().methodName().stringValue();
        boolean supported = (getterName.startsWith("get") || getterName.startsWith("is"))
                && getter.parameterCount() == 0
                && !getter.returnType().equals(ConstantDescs.CD_void)
                && pair.setter().methodName().stringValue().equals("set" + DiscoveryRules.titleCase(pair.property()))
                && setter.parameterCount() == 1
                && setter.parameterType(0).equals(getter.returnType())
                && isCallable(pair.getter().flags().flagsMask(), declaringClass, entityType, standin)
                && isCallable(pair.setter().flags().flagsMask(), declaringClass, entityType, standin);
        if (!supported) {
            throw new UnsupportedOperationException("Critter can't generate accessors for %s's %s() and %s()".formatted(
                    declaringClass.getName(), getterName, pair.setter().methodName().stringValue()));
        }
        return toMethodInfo(pair.getter(), declaringClass).withSetter(toMethodInfo(pair.setter(), declaringClass));
    }

    /**
     * Whether the generated accessors can call a method. Runtime accessors are nestmates of the declaring class, so they
     * can call any. A method woven into the entity can call one the entity declares, a public or protected one it
     * inherits, or a package-private one declared in the entity's package by the same loader. An {@code @ExternalEntity}
     * stand-in's accessors call the target's methods of the same name, found as a non-private one.
     */
    private boolean isCallable(int flags, Class<?> declaringClass, Class<?> entityType, boolean standin) {
        if (standin) {
            return (flags & ClassFile.ACC_PRIVATE) == 0;
        }
        if (runtimeMode || declaringClass == entityType || (flags & (ClassFile.ACC_PUBLIC | ClassFile.ACC_PROTECTED)) != 0) {
            return true;
        }
        return (flags & ClassFile.ACC_PRIVATE) == 0
                && declaringClass.getPackageName().equals(entityType.getPackageName())
                && declaringClass.getClassLoader() == entityType.getClassLoader();
    }

    private MethodInfo toMethodInfo(MethodModel method, Class<?> declaringClass) {
        String sig = method.findAttribute(signature())
                .map(a -> a.signature().stringValue())
                .orElse(null);
        return new MethodInfo(
                method.methodName().stringValue(),
                method.methodType().stringValue(),
                sig,
                method.flags().flagsMask(),
                visibleAnnotations(method),
                declaringClass);
    }

    private List<Annotation> visibleAnnotations(io.smallrye.classfile.AttributedElement element) {
        return element.findAttribute(runtimeVisibleAnnotations())
                .map(RuntimeVisibleAnnotationsAttribute::annotations)
                .orElse(List.of());
    }
}
