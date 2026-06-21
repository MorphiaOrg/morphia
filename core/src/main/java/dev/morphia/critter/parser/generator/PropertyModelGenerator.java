package dev.morphia.critter.parser.generator;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.DBRef;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Reference;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.conventions.PropertyConvention;
import dev.morphia.critter.parser.ExtensionFunctions;
import dev.morphia.critter.parser.FieldInfo;
import dev.morphia.critter.parser.MethodInfo;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.bson.codecs.pojo.PropertyAccessor;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

/**
 * Generates a ClassFile-based {@link dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel} implementation
 * for a single property of a Morphia entity class.
 */
public class PropertyModelGenerator extends BaseGenerator {
    private final MorphiaConfig config;
    private final String propertyName;
    private final String accessorType;
    private final boolean isFieldBased;
    private final int accessFlags;
    private final java.lang.reflect.Type genericType;
    private final Map<String, Annotation> annotationMap;
    private final TypeData<?> typeData;
    private final String getterName;

    /**
     * Creates a generator for a field-based property.
     *
     * @param annotationSource the class to reflect on for field annotations (may differ from entity for @ExternalEntity stand-ins)
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, Class<?> annotationSource,
            CritterClassLoader critterClassLoader, FieldInfo field) {
        super(entity, critterClassLoader);
        this.config = config;
        this.isFieldBased = true;
        this.propertyName = field.name();
        generatedType = "%s.%sModel".formatted(baseName, Critter.titleCase(propertyName));
        accessorType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));

        Field reflectedField = findField(annotationSource, field.name());
        this.accessFlags = reflectedField != null ? reflectedField.getModifiers() : field.access();
        this.genericType = reflectedField != null ? reflectedField.getGenericType() : Object.class;
        this.annotationMap = buildAnnotationMap(reflectedField != null ? reflectedField.getAnnotations() : new Annotation[0]);
        this.typeData = TypeData.get(resolveGenericType(this.genericType, field.name(), annotationSource));
        this.getterName = null;
    }

    /**
     * Creates a generator for a field-based property where the entity class is also the annotation source.
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, CritterClassLoader critterClassLoader, FieldInfo field) {
        this(config, entity, entity, critterClassLoader, field);
    }

    /**
     * Creates a generator for a method-based (getter) property.
     *
     * @param annotationSource the class to reflect on for method annotations (may differ from entity for @ExternalEntity stand-ins)
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, Class<?> annotationSource,
            CritterClassLoader critterClassLoader, MethodInfo method) {
        super(entity, critterClassLoader);
        this.config = config;
        this.isFieldBased = false;
        this.propertyName = ExtensionFunctions.getterToPropertyName(method, entity);
        generatedType = "%s.%sModel".formatted(baseName, Critter.titleCase(propertyName));
        accessorType = "%s.%sAccessor".formatted(baseName, Critter.titleCase(propertyName));

        Method reflectedMethod = findMethod(annotationSource, method.name());
        this.accessFlags = reflectedMethod != null ? reflectedMethod.getModifiers() : method.access();
        this.genericType = reflectedMethod != null ? reflectedMethod.getGenericReturnType() : Object.class;
        this.annotationMap = buildAnnotationMap(reflectedMethod != null ? reflectedMethod.getAnnotations() : new Annotation[0]);
        // Also collect setter annotations — some annotations (e.g. @Version, @Text) live on the setter, not the getter
        String setterName = "set" + Critter.titleCase(this.propertyName);
        Method reflectedSetter = findSetterMethod(annotationSource, setterName);
        if (reflectedSetter != null) {
            for (Annotation ann : reflectedSetter.getAnnotations()) {
                this.annotationMap.putIfAbsent(ann.annotationType().getName(), ann);
            }
        }
        this.typeData = TypeData.get(resolveGenericType(this.genericType, this.propertyName, annotationSource));
        this.getterName = method.name();
    }

    /**
     * Creates a generator for a method-based property where the entity class is also the annotation source.
     */
    public PropertyModelGenerator(MorphiaConfig config, Class<?> entity, CritterClassLoader critterClassLoader, MethodInfo method) {
        this(config, entity, entity, critterClassLoader, method);
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

    private static Method findSetterMethod(Class<?> cls, String setterName) {
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1 && !m.isBridge()) {
                    return m;
                }
            }
            current = current.getSuperclass();
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

    private static java.lang.reflect.Type resolveGenericType(java.lang.reflect.Type type, String memberName, Class<?> entity) {
        if (!(type instanceof TypeVariable<?> tv)) {
            return type;
        }
        Class<?> declaringClass = findDeclaringClass(memberName, entity);
        Class<?> resolved = resolveTypeVariable(tv.getName(), entity, declaringClass);
        return (resolved != null && resolved != Object.class) ? resolved : type;
    }

    private static Class<?> findDeclaringClass(String memberName, Class<?> concreteClass) {
        Class<?> current = concreteClass;
        while (current != null && current != Object.class) {
            try {
                current.getDeclaredField(memberName);
                return current;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        // Fall back to getter for method-based properties
        String titleName = Critter.titleCase(memberName);
        current = concreteClass;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                String name = m.getName();
                if ((name.equals("get" + titleName) || name.equals("is" + titleName))
                        && m.getParameterCount() == 0 && !m.isBridge()) {
                    return current;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Class<?> resolveTypeVariable(String typeVarName, Class<?> concreteClass, Class<?> declaringClass) {
        Map<String, java.lang.reflect.Type> bindings = new HashMap<>();
        Class<?> current = concreteClass;
        while (current != null && current != Object.class) {
            java.lang.reflect.Type genericSuper = current.getGenericSuperclass();
            Class<?> superClass = current.getSuperclass();
            if (superClass == null || superClass == Object.class) {
                break;
            }
            if (genericSuper instanceof ParameterizedType paramType) {
                TypeVariable<?>[] typeParams = superClass.getTypeParameters();
                java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                for (int i = 0; i < typeParams.length && i < typeArgs.length; i++) {
                    java.lang.reflect.Type arg = typeArgs[i];
                    if (arg instanceof TypeVariable<?> argTv && bindings.containsKey(argTv.getName())) {
                        arg = bindings.get(argTv.getName());
                    }
                    bindings.put(typeParams[i].getName(), arg);
                }
            }
            if (declaringClass != null && superClass == declaringClass) {
                break;
            }
            current = superClass;
        }
        java.lang.reflect.Type resolved = bindings.get(typeVarName);
        return resolved instanceof Class<?> c ? c : null;
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
            io.github.dmlloyd.classfile.Signature sig = io.github.dmlloyd.classfile.Signature.parseFrom(input);
            TypeData<?> result = typeDataFromSignature(sig, classLoader);
            return result != null ? List.of(result) : java.util.Collections.emptyList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    private static TypeData<?> typeDataFromSignature(io.github.dmlloyd.classfile.Signature sig, ClassLoader classLoader) {
        if (sig instanceof io.github.dmlloyd.classfile.Signature.ClassTypeSig cts) {
            java.lang.constant.ClassDesc cd = cts.classDesc();
            Class<?> raw = GenerationUtils.asClass(cd, classLoader);
            @SuppressWarnings("unchecked")
            List<TypeData<?>> params = (List<TypeData<?>>) (List<?>) cts.typeArgs().stream()
                    .map(arg -> typeDataFromTypeArg(arg, classLoader))
                    .toList();
            return new TypeData<>(raw, params);
        } else if (sig instanceof io.github.dmlloyd.classfile.Signature.ArrayTypeSig ats) {
            TypeData<?> component = typeDataFromSignature(ats.componentSignature(), classLoader);
            if (component == null)
                return new TypeData<>(Object.class, List.of());
            try {
                Class<?> arrayClass = java.lang.reflect.Array.newInstance(component.getType(), 0).getClass();
                return new TypeData<>(arrayClass, List.of());
            } catch (Exception e) {
                return new TypeData<>(Object.class, List.of());
            }
        } else if (sig instanceof io.github.dmlloyd.classfile.Signature.BaseTypeSig bts) {
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
        } else if (sig instanceof io.github.dmlloyd.classfile.Signature.TypeVarSig) {
            return new TypeData<>(Object.class, List.of());
        }
        return new TypeData<>(Object.class, List.of());
    }

    private static TypeData<?> typeDataFromTypeArg(io.github.dmlloyd.classfile.Signature.TypeArg arg, ClassLoader classLoader) {
        if (arg instanceof io.github.dmlloyd.classfile.Signature.TypeArg.Bounded bounded) {
            return typeDataFromSignature(bounded.boundType(), classLoader);
        }
        return new TypeData<>(Object.class, List.of());
    }

    /**
     * Emits the generated property model class and returns this generator.
     */
    public PropertyModelGenerator emit() {
        ClassDesc thisDesc = ClassDesc.of(generatedType);
        ClassDesc superDesc = ClassDesc.of(CritterPropertyModel.class.getName());
        ClassDesc entityModelDesc = ClassDesc.of(EntityModel.class.getName());
        ClassDesc propertyModelDesc = ClassDesc.of(PropertyModel.class.getName());
        ClassDesc accessorDesc = ClassDesc.of(PropertyAccessor.class.getName());
        ClassDesc accessorImplDesc = ClassDesc.of(accessorType);
        ClassDesc typeDataDesc = ClassDesc.of(TypeData.class.getName());
        ClassDesc annotationDesc = ClassDesc.of(Annotation.class.getName());

        boolean isFinalFlag = java.lang.reflect.Modifier.isFinal(accessFlags);
        boolean isTransientFlag = java.lang.reflect.Modifier.isTransient(accessFlags)
                || PropertyConvention.transientAnnotations().stream().anyMatch(c -> annotationMap.containsKey(c.getName()));
        boolean isReferenceFlag = DBRef.class.isAssignableFrom(typeData.getType())
                || annotationMap.containsKey(Reference.class.getName());
        boolean isArrayFlag = typeData.getType().isArray();
        boolean isMapFlag = Map.class.isAssignableFrom(typeData.getType());
        boolean isSetFlag = java.util.Set.class.isAssignableFrom(typeData.getType());
        boolean isCollectionFlag = java.util.Collection.class.isAssignableFrom(typeData.getType());
        String mappedName = PropertyConvention.mappedName(config, annotationMap, propertyName);
        Class<?> normalizedType = PropertyModel.normalize(typeData);
        AlsoLoad alsoLoad = (AlsoLoad) annotationMap.get(AlsoLoad.class.getName());
        String[] loadNamesArr = alsoLoad != null ? alsoLoad.value() : new String[0];

        List<Annotation> morphiaAnnotations = annotationMap.values().stream()
                .filter(a -> a.annotationType().getName().startsWith("dev.morphia.annotations."))
                .toList();
        List<Annotation> nonMorphiaAnnotations = annotationMap.values().stream()
                .filter(a -> !a.annotationType().getName().startsWith("dev.morphia.annotations."))
                .toList();
        List<io.github.dmlloyd.classfile.Annotation> cfAnnotations = nonMorphiaAnnotations.stream()
                .map(GenerationUtils::toClassfileAnnotation)
                .toList();

        byte[] bytes = ClassFile.of().build(thisDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            cb.withSuperclass(superDesc);
            if (!cfAnnotations.isEmpty()) {
                cb.with(RuntimeVisibleAnnotationsAttribute.of(cfAnnotations));
            }

            cb.withField("entityModel", entityModelDesc, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);
            cb.withField("accessor", accessorImplDesc, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);

            // Constructor: (EntityModel) -> void
            cb.withMethodBody("<init>", MethodTypeDesc.of(ConstantDescs.CD_void, entityModelDesc),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(0);
                        cod.aload(1);
                        cod.invokespecial(superDesc, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void, entityModelDesc));

                        cod.aload(0);
                        cod.aload(1);
                        cod.putfield(thisDesc, "entityModel", entityModelDesc);

                        cod.aload(0);
                        cod.new_(accessorImplDesc);
                        cod.dup();
                        cod.invokespecial(accessorImplDesc, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                        cod.putfield(thisDesc, "accessor", accessorImplDesc);

                        // Morphia annotations: builder with hardcoded values — no reflection
                        for (Annotation ann : morphiaAnnotations) {
                            cod.aload(0);
                            GenerationUtils.emitAnnotationViaBuilder(cod, ann);
                            cod.invokevirtual(propertyModelDesc, "annotation",
                                    MethodTypeDesc.of(propertyModelDesc, annotationDesc));
                            cod.pop();
                        }

                        // Non-Morphia annotations: on the generated class via RuntimeVisibleAnnotationsAttribute
                        for (Annotation ann : nonMorphiaAnnotations) {
                            cod.aload(0);
                            cod.aload(0);
                            cod.invokevirtual(ConstantDescs.CD_Object, "getClass",
                                    MethodTypeDesc.of(ConstantDescs.CD_Class));
                            GenerationUtils.emitClassRef(cod, ann.annotationType());
                            cod.invokevirtual(ConstantDescs.CD_Class, "getDeclaredAnnotation",
                                    MethodTypeDesc.ofDescriptor(
                                            "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;"));
                            // getDeclaredAnnotation returns null if the annotation is absent at runtime
                            // (e.g. non-RUNTIME retention). Guard to avoid NPE in PropertyModel.annotation().
                            var skipLabel = cod.newLabel();
                            var endLabel = cod.newLabel();
                            cod.dup();
                            cod.ifnull(skipLabel);
                            cod.invokevirtual(propertyModelDesc, "annotation",
                                    MethodTypeDesc.of(propertyModelDesc, annotationDesc));
                            cod.pop();
                            cod.goto_(endLabel);
                            cod.labelBinding(skipLabel);
                            cod.pop(); // pop null
                            cod.pop(); // pop this
                            cod.labelBinding(endLabel);
                        }

                        cod.return_();
                    });

            // getAccessor(): PropertyAccessor
            cb.withMethodBody("getAccessor", MethodTypeDesc.of(accessorDesc),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(0);
                        cod.getfield(thisDesc, "accessor", accessorImplDesc);
                        cod.areturn();
                    });

            // getEntityModel(): EntityModel
            cb.withMethodBody("getEntityModel", MethodTypeDesc.of(entityModelDesc),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.aload(0);
                        cod.getfield(thisDesc, "entityModel", entityModelDesc);
                        cod.areturn();
                    });

            // getFullName(): String
            cb.withMethodBody("getFullName", MethodTypeDesc.of(ConstantDescs.CD_String),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.ldc("%s#%s".formatted(entity.getName(), propertyName));
                        cod.areturn();
                    });

            // getName(): String
            cb.withMethodBody("getName", MethodTypeDesc.of(ConstantDescs.CD_String),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.ldc(propertyName);
                        cod.areturn();
                    });

            // getMappedName(): String
            cb.withMethodBody("getMappedName", MethodTypeDesc.of(ConstantDescs.CD_String),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.ldc(mappedName);
                        cod.areturn();
                    });

            // getLoadNames(): List
            cb.withMethodBody("getLoadNames", MethodTypeDesc.of(ClassDesc.of("java.util.List")),
                    ClassFile.ACC_PUBLIC, cod -> {
                        cod.loadConstant(loadNamesArr.length);
                        cod.anewarray(ConstantDescs.CD_Object);
                        for (int i = 0; i < loadNamesArr.length; i++) {
                            cod.dup();
                            cod.loadConstant(i);
                            cod.ldc(loadNamesArr[i]);
                            cod.aastore();
                        }
                        cod.invokestatic(ClassDesc.of("java.util.Arrays"), "asList",
                                MethodTypeDesc.ofDescriptor("([Ljava/lang/Object;)Ljava/util/List;"));
                        cod.areturn();
                    });

            // getNormalizedType(): Class
            cb.withMethodBody("getNormalizedType", MethodTypeDesc.of(ConstantDescs.CD_Class),
                    ClassFile.ACC_PUBLIC, cod -> {
                        GenerationUtils.emitClassRef(cod, normalizedType);
                        cod.areturn();
                    });

            // getType(): Class
            cb.withMethodBody("getType", MethodTypeDesc.of(ConstantDescs.CD_Class),
                    ClassFile.ACC_PUBLIC, cod -> {
                        GenerationUtils.emitClassRef(cod, typeData.getType());
                        cod.areturn();
                    });

            // getTypeData(): TypeData
            cb.withMethodBody("getTypeData", MethodTypeDesc.of(typeDataDesc),
                    ClassFile.ACC_PUBLIC, cod -> {
                        GenerationUtils.emitTypeData(typeData, cod);
                        cod.areturn();
                    });

            // isArray(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isArray", isArrayFlag);
            // isFinal(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isFinal", isFinalFlag);
            // isReference(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isReference", isReferenceFlag);
            // isTransient(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isTransient", isTransientFlag);
            // isMap(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isMap", isMapFlag);
            // isSet(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isSet", isSetFlag);
            // isCollection(): boolean
            GenerationUtils.emitBooleanMethod(cb, "isCollection", isCollectionFlag);
        });

        critterClassLoader.register(generatedType, bytes);
        return this;
    }

}
