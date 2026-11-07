package dev.morphia.critter.parser.generator;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IdField;
import dev.morphia.annotations.Property;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.FieldInfo;
import dev.morphia.critter.parser.MethodInfo;
import dev.morphia.mapping.codec.ArrayAccessor;
import dev.morphia.mapping.codec.pojo.ClassHierarchy;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.TypeResolver;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.mapping.conventions.DiscoveryRules;

import org.bson.codecs.pojo.PropertyAccessor;

import io.smallrye.classfile.ClassBuilder;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.CodeBuilder;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

/**
 * Generates the code, in a generated entity model's constructor, that builds the model of one property of a Morphia
 * entity class.
 */
public class PropertyModelGenerator extends BaseGenerator {
    private final MorphiaConfig config;
    private final String propertyName;
    private final String accessorType;
    private final boolean isFieldBased;
    private final boolean runtimeMode;
    private final int accessFlags;
    private final java.lang.reflect.Type genericType;
    private final Map<String, Annotation> annotationMap;
    private final TypeData<?> typeData;
    // the component type of an array property whose values need converting, as for ArrayAccessor
    @Nullable
    private final Class<?> arrayComponent;
    @Nullable
    private String idFieldName;

    /**
     * Creates a generator for a field-based property.
     *
     * @param annotationSource the class to reflect on for field annotations (may differ from entity for @ExternalEntity stand-ins)
     * @param runtimeMode      {@code true} to use the nestmate accessor registry instead of generating {@code new} bytecode
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, Class<?> annotationSource,
            CritterClassLoader critterClassLoader, FieldInfo field, boolean runtimeMode) {
        super(entity, critterClassLoader);
        this.config = config;
        this.isFieldBased = true;
        this.runtimeMode = runtimeMode;
        this.propertyName = field.name();
        accessorType = "%s.%sAccessor".formatted(baseName, DiscoveryRules.titleCase(propertyName));

        Field reflectedField = findField(annotationSource, field.name());
        this.accessFlags = reflectedField != null ? reflectedField.getModifiers() : field.access();
        this.genericType = reflectedField != null ? reflectedField.getGenericType() : Object.class;
        this.annotationMap = buildAnnotationMap(reflectedField != null ? reflectedField.getAnnotations() : new Annotation[0]);
        this.typeData = typeData(annotationSource, genericType, annotationMap);
        this.arrayComponent = arrayComponent(reflectedField != null ? reflectedField.getType() : null);
        this.idFieldName = idFieldName(annotationSource);
    }

    /**
     * Creates a generator for a field-based property where the entity class is also the annotation source.
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, Class<?> annotationSource,
            CritterClassLoader critterClassLoader, FieldInfo field) {
        this(config, entity, annotationSource, critterClassLoader, field, false);
    }

    /**
     * Creates a generator for a field-based property where the entity class is also the annotation source.
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, CritterClassLoader critterClassLoader, FieldInfo field) {
        this(config, entity, entity, critterClassLoader, field, false);
    }

    /**
     * Creates a generator for a method-based (getter) property.
     *
     * @param annotationSource the class to reflect on for method annotations (may differ from entity for @ExternalEntity stand-ins)
     * @param runtimeMode      {@code true} to use the nestmate accessor registry instead of generating {@code new} bytecode
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, Class<?> annotationSource,
            CritterClassLoader critterClassLoader, MethodInfo method, boolean runtimeMode) {
        super(entity, critterClassLoader);
        this.config = config;
        this.isFieldBased = false;
        this.runtimeMode = runtimeMode;
        this.propertyName = DiscoveryRules.propertyName(method.name());
        accessorType = "%s.%sAccessor".formatted(baseName, DiscoveryRules.titleCase(propertyName));

        // the pair's own class: a subclass's override of the getter, if any, isn't part of the pair
        Class<?> declaringClass = method.declaringClass() != null ? method.declaringClass() : annotationSource;
        Method reflectedMethod = findMethod(declaringClass, method.name());
        this.accessFlags = reflectedMethod != null ? reflectedMethod.getModifiers() : method.access();
        this.genericType = reflectedMethod != null ? reflectedMethod.getGenericReturnType() : Object.class;
        this.annotationMap = buildAnnotationMap(reflectedMethod != null ? reflectedMethod.getAnnotations() : new Annotation[0]);
        // Also collect setter annotations — some annotations (e.g. @Version, @Text) live on the setter, not the getter
        String setterName = "set" + DiscoveryRules.titleCase(this.propertyName);
        Method reflectedSetter = null;
        if (method.setter() != null) {
            // the paired setter, which may be private
            reflectedSetter = findSetter(declaringClass, method.setter());
        } else if (reflectedMethod != null) {
            Class<?> paramType = reflectedMethod.getReturnType();
            ClassDesc paramDesc = paramType.isPrimitive()
                    ? GenerationUtils.primitiveClassDesc(paramType.getName())
                    : ClassDesc.of(paramType.getName());
            reflectedSetter = GenerationUtils.findSetterMethod(declaringClass, setterName, paramDesc);
        }
        if (reflectedSetter != null) {
            for (Annotation ann : reflectedSetter.getAnnotations()) {
                // as for reflection, the setter's annotation wins over the getter's of the same type
                this.annotationMap.put(ann.annotationType().getName(), ann);
            }
        }
        this.typeData = typeData(annotationSource, genericType, annotationMap);
        this.arrayComponent = arrayComponent(reflectedMethod != null ? reflectedMethod.getReturnType() : null);
        this.idFieldName = idFieldName(annotationSource);
    }

    /**
     * Creates a generator for a method-based (getter) property.
     *
     * @param annotationSource the class to reflect on for method annotations (may differ from entity for @ExternalEntity stand-ins)
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, Class<?> annotationSource,
            CritterClassLoader critterClassLoader, MethodInfo method) {
        this(config, entity, annotationSource, critterClassLoader, method, false);
    }

    /**
     * Creates a generator for a method-based property where the entity class is also the annotation source.
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, CritterClassLoader critterClassLoader, MethodInfo method) {
        this(config, entity, entity, critterClassLoader, method, false);
    }

    private static Field findField(Class<?> cls, String name) {
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name) {
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0 && !m.isBridge()) {
                    return m;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Method findSetter(Class<?> declaringClass, MethodInfo setter) {
        for (Method m : declaringClass.getDeclaredMethods()) {
            if (m.getName().equals(setter.name()) && MethodTypeDesc.ofDescriptor(setter.desc()).equals(
                    MethodType.methodType(m.getReturnType(), m.getParameterTypes()).describeConstable().orElse(null))) {
                return m;
            }
        }
        return null;
    }

    private static Map<String, Annotation> buildAnnotationMap(Annotation[] annotations) {
        Map<String, Annotation> map = new LinkedHashMap<>();
        for (Annotation ann : annotations) {
            map.put(ann.annotationType().getName(), ann);
        }
        return map;
    }

    /**
     * Parses a classfile signature string into a list of {@link TypeData} instances.
     * Uses the ClassFile API's {@code Signature.parseFrom()} for type argument extraction.
     *
     * @param input       the classfile type signature to parse
     * @param classLoader the class loader used to resolve referenced types
     * @return a single-element list containing the parsed type data, or empty if parsing fails
     */
    public static List<TypeData<?>> typeData(String input, ClassLoader classLoader) {
        if (input == null || input.isEmpty())
            return java.util.Collections.emptyList();
        try {
            io.smallrye.classfile.Signature sig = io.smallrye.classfile.Signature.parseFrom(input);
            TypeData<?> result = typeDataFromSignature(sig, classLoader);
            return result != null ? List.of(result) : java.util.Collections.emptyList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    private static TypeData<?> typeDataFromSignature(io.smallrye.classfile.Signature sig, ClassLoader classLoader) {
        if (sig instanceof io.smallrye.classfile.Signature.ClassTypeSig cts) {
            java.lang.constant.ClassDesc cd = cts.classDesc();
            Class<?> raw = GenerationUtils.asClass(cd, classLoader);
            @SuppressWarnings("unchecked")
            List<TypeData<?>> params = (List<TypeData<?>>) (List<?>) cts.typeArgs().stream()
                    .map(arg -> typeDataFromTypeArg(arg, classLoader))
                    .toList();
            return new TypeData<>(raw, params);
        } else if (sig instanceof io.smallrye.classfile.Signature.ArrayTypeSig ats) {
            TypeData<?> component = typeDataFromSignature(ats.componentSignature(), classLoader);
            if (component == null)
                return new TypeData<>(Object.class, List.of());
            try {
                Class<?> arrayClass = java.lang.reflect.Array.newInstance(component.getType(), 0).getClass();
                return new TypeData<>(arrayClass, List.of());
            } catch (Exception e) {
                return new TypeData<>(Object.class, List.of());
            }
        } else if (sig instanceof io.smallrye.classfile.Signature.BaseTypeSig bts) {
            Class<?> primitive = switch (bts.baseType()) {
                case 'Z' -> boolean.class;
                case 'C' -> char.class;
                case 'B' -> byte.class;
                case 'S' -> short.class;
                case 'I' -> int.class;
                case 'J' -> long.class;
                case 'F' -> float.class;
                case 'D' -> double.class;
                case 'V' -> void.class;
                default -> Object.class;
            };
            return new TypeData<>(primitive, List.of());
        } else if (sig instanceof io.smallrye.classfile.Signature.TypeVarSig) {
            return new TypeData<>(Object.class, List.of());
        }
        return new TypeData<>(Object.class, List.of());
    }

    private static TypeData<?> typeDataFromTypeArg(io.smallrye.classfile.Signature.TypeArg arg, ClassLoader classLoader) {
        if (arg instanceof io.smallrye.classfile.Signature.TypeArg.Bounded bounded) {
            return typeDataFromSignature(bounded.boundType(), classLoader);
        }
        return new TypeData<>(Object.class, List.of());
    }

    /**
     * As in {@code ConfigureProperties}, {@code @Property(concreteClass)} replaces the declared type, keeping its type
     * arguments.
     */
    private static TypeData<?> typeData(Class<?> annotationSource, java.lang.reflect.Type genericType,
            Map<String, Annotation> annotations) {
        TypeData<?> typeData = TypeData.get(TypeResolver.resolve(annotationSource, genericType));
        Property property = (Property) annotations.get(Property.class.getName());
        return property != null && !property.concreteClass().equals(Object.class)
                ? typeData.withType(property.concreteClass())
                : typeData;
    }

    @Nullable
    private static Class<?> arrayComponent(@Nullable Class<?> type) {
        return type != null && ArrayAccessor.converts(type) ? type.getComponentType() : null;
    }

    @Nullable
    private static String idFieldName(Class<?> annotationSource) {
        IdField idField = (IdField) ClassHierarchy.annotations(annotationSource).get(IdField.class);
        return idField != null ? idField.value() : null;
    }

    /**
     * @return the property's Java name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return true if the property is annotated {@code @Id}
     */
    public boolean isAnnotatedId() {
        return annotationMap.containsKey(Id.class.getName());
    }

    /**
     * Leaves the entity's {@code @IdField} out of this property's mapped name, because another property is annotated
     * {@code @Id}: as in {@code ConfigureProperties}, an explicit {@code @Id} wins. Call before the entity model is emitted.
     *
     * @return this
     */
    public PropertyModelGenerator ignoreIdField() {
        idFieldName = null;
        return this;
    }

    /**
     * @return the name of the field in the generated entity model that carries this property's annotations other than
     *         Morphia's, or null if it has none
     */
    @Nullable
    String annotationHolder() {
        return foreignAnnotations().isEmpty() ? null : "__annotations$" + propertyName;
    }

    /**
     * Adds the field named by {@link #annotationHolder()} to the generated entity model, if the property needs one. Only
     * Morphia annotations have builders, so the others are carried as the field's own annotations.
     */
    void emitAnnotationHolder(ClassBuilder cb) {
        String holder = annotationHolder();
        if (holder != null) {
            List<io.smallrye.classfile.Annotation> annotations = foreignAnnotations().stream()
                    .map(GenerationUtils::toClassfileAnnotation)
                    .toList();
            cb.withField(holder, ConstantDescs.CD_Object, fb -> fb
                    .withFlags(ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC)
                    .with(RuntimeVisibleAnnotationsAttribute.of(annotations)));
        }
    }

    /**
     * Emits, into a method of the generated entity model that its constructor calls, code that builds this property's
     * model as a plain {@link PropertyModel} and adds it to the entity model in local 0. Every value is a constant or is resolved here,
     * once, so the model's getters are the base class's.
     *
     * @param cod       the method's code
     * @param modelDesc the generated entity model, whose class loader resolves non-public types
     * @param slot      a free local variable slot
     */
    void emitProperty(CodeBuilder cod, ClassDesc modelDesc, int slot) {
        ClassDesc propertyModelDesc = ClassDesc.of(PropertyModel.class.getName());
        ClassDesc entityModelDesc = ClassDesc.of(EntityModel.class.getName());
        ClassDesc critterModelDesc = ClassDesc.of(CritterEntityModel.class.getName());
        ClassDesc accessorDesc = ClassDesc.of(PropertyAccessor.class.getName());
        ClassDesc accessorImplDesc = ClassDesc.of(accessorType);
        ClassDesc typeDataDesc = ClassDesc.of(TypeData.class.getName());
        ClassDesc annotationDesc = ClassDesc.of(Annotation.class.getName());

        // @IdField names the id property at the entity level, so the property's own annotations don't show it
        String mappedName = propertyName.equals(idFieldName) ? "_id"
                : DiscoveryRules.mappedName(config, type -> annotationMap.get(type.getName()), propertyName);
        AlsoLoad alsoLoad = (AlsoLoad) annotationMap.get(AlsoLoad.class.getName());

        cod.new_(propertyModelDesc);
        cod.dup();
        cod.aload(0);
        cod.invokespecial(propertyModelDesc, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void, entityModelDesc));
        cod.astore(slot);

        cod.aload(slot);
        cod.ldc(propertyName);
        cod.invokevirtual(propertyModelDesc, "name", MethodTypeDesc.of(propertyModelDesc, ConstantDescs.CD_String));
        cod.pop();

        cod.aload(slot);
        GenerationUtils.emitTypeData(typeData, cod, modelDesc);
        cod.invokevirtual(propertyModelDesc, "typeData", MethodTypeDesc.of(propertyModelDesc, typeDataDesc));
        cod.pop();

        cod.aload(slot);
        cod.loadConstant(java.lang.reflect.Modifier.isFinal(accessFlags) && isFieldBased ? 1 : 0);
        cod.invokevirtual(propertyModelDesc, "isFinal", MethodTypeDesc.of(propertyModelDesc, ConstantDescs.CD_boolean));
        cod.pop();

        cod.aload(slot);
        if (runtimeMode) {
            // the pre-instantiated nestmate accessor registered for the generated model's loader
            ClassDesc registryDesc = ClassDesc.of(NestmateAccessorRegistry.class.getName());
            ClassDesc classLoaderDesc = ClassDesc.of(ClassLoader.class.getName());
            cod.ldc(modelDesc);
            cod.invokevirtual(ConstantDescs.CD_Class, "getClassLoader", MethodTypeDesc.of(classLoaderDesc));
            cod.ldc(accessorType);
            cod.invokestatic(registryDesc, "get", MethodTypeDesc.of(accessorDesc, classLoaderDesc, ConstantDescs.CD_String));
        } else {
            cod.new_(accessorImplDesc);
            cod.dup();
            cod.invokespecial(accessorImplDesc, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void));
        }
        cod.invokevirtual(propertyModelDesc, "accessor", MethodTypeDesc.of(propertyModelDesc, accessorDesc));
        cod.pop();
        if (arrayComponent != null) {
            cod.aload(0);
            cod.aload(slot);
            GenerationUtils.emitClassRef(cod, arrayComponent, modelDesc);
            cod.invokevirtual(critterModelDesc, "arrayAccessor",
                    MethodTypeDesc.of(ConstantDescs.CD_void, propertyModelDesc, ConstantDescs.CD_Class));
        }

        // Morphia annotations through their builders: no reflection
        for (Annotation annotation : annotationMap.values()) {
            if (isMorphiaAnnotation(annotation)) {
                cod.aload(slot);
                GenerationUtils.emitAnnotationViaBuilder(cod, annotation, modelDesc);
                cod.invokevirtual(propertyModelDesc, "annotation", MethodTypeDesc.of(propertyModelDesc, annotationDesc));
                cod.pop();
            }
        }
        String holder = annotationHolder();
        if (holder != null) {
            cod.aload(0);
            cod.aload(slot);
            cod.ldc(holder);
            cod.invokevirtual(critterModelDesc, "foreignAnnotations",
                    MethodTypeDesc.of(ConstantDescs.CD_void, propertyModelDesc, ConstantDescs.CD_String));
        }

        cod.aload(slot);
        cod.ldc(mappedName);
        cod.invokevirtual(propertyModelDesc, "mappedName", MethodTypeDesc.of(propertyModelDesc, ConstantDescs.CD_String));
        cod.pop();

        cod.aload(0);
        cod.aload(slot);
        cod.invokevirtual(entityModelDesc, "addProperty", MethodTypeDesc.of(ConstantDescs.CD_boolean, propertyModelDesc));
        cod.pop();

        // load names go after the property is added: they're registered with the entity model
        if (alsoLoad != null && alsoLoad.value().length > 0) {
            String[] names = alsoLoad.value();
            cod.aload(slot);
            cod.loadConstant(names.length);
            cod.anewarray(ConstantDescs.CD_String);
            for (int i = 0; i < names.length; i++) {
                cod.dup();
                cod.loadConstant(i);
                cod.ldc(names[i]);
                cod.aastore();
            }
            cod.invokevirtual(propertyModelDesc, "alternateNames",
                    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String.arrayType()));
        }
    }

    private List<Annotation> foreignAnnotations() {
        return annotationMap.values().stream()
                .filter(a -> !isMorphiaAnnotation(a))
                .toList();
    }

    private static boolean isMorphiaAnnotation(Annotation annotation) {
        return annotation.annotationType().getName().startsWith("dev.morphia.annotations.");
    }
}
