package dev.morphia.critter.parser.generator;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.codec.pojo.TypeData;

import io.github.dmlloyd.classfile.ClassBuilder;
import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

/**
 * Static utility methods bridging annotation introspection and Morphia type data with the ClassFile API.
 */
public class GenerationUtils {

    public static final Map<String, String> PRIMITIVE_TO_WRAPPER = Map.of(
            "boolean", "java.lang.Boolean",
            "byte", "java.lang.Byte",
            "char", "java.lang.Character",
            "short", "java.lang.Short",
            "int", "java.lang.Integer",
            "long", "java.lang.Long",
            "float", "java.lang.Float",
            "double", "java.lang.Double");

    private GenerationUtils() {
    }

    /**
     * Converts a ClassDesc to a type class name string suitable for Class.forName().
     */
    public static String typeClassName(ClassDesc cd) {
        String desc = cd.descriptorString();
        if (desc.length() == 1) {
            return switch (desc.charAt(0)) {
                case 'Z' -> "boolean";
                case 'C' -> "char";
                case 'B' -> "byte";
                case 'S' -> "short";
                case 'I' -> "int";
                case 'J' -> "long";
                case 'F' -> "float";
                case 'D' -> "double";
                default -> throw new IllegalArgumentException("Unknown primitive: " + desc);
            };
        }
        if (desc.startsWith("[")) {
            return desc.replace('/', '.');
        }
        return desc.substring(1, desc.length() - 1).replace('/', '.');
    }

    /**
     * Emits a no-arg boolean method that returns a constant value.
     */
    public static void emitBooleanMethod(ClassBuilder cb, String name, boolean value) {
        cb.withMethodBody(name, MethodTypeDesc.ofDescriptor("()Z"), ClassFile.ACC_PUBLIC, cod -> {
            cod.loadConstant(value ? 1 : 0);
            cod.return_(TypeKind.INT);
        });
    }

    /**
     * Returns the classloader for the given type, falling back to the system classloader for bootstrap-loaded types.
     */
    public static ClassLoader safeClassLoader(Class<?> type) {
        ClassLoader cl = type.getClassLoader();
        return cl != null ? cl : ClassLoader.getSystemClassLoader();
    }

    /**
     * Generates a concrete class that implements the given annotation interface with all element
     * values hardcoded as constants. The class is registered with the given class loader and its
     * ClassDesc is returned for use in {@code new} bytecode instructions.
     *
     * <p>
     * This is used to emit annotation instances inline into generated property/entity model
     * constructors, eliminating all runtime reflection.
     */
    public static ClassDesc generateAnnotationImpl(Annotation ann, String baseName, CritterClassLoader classLoader) {
        Class<?> annType = ann.annotationType();
        String implName = baseName + "$$" + annType.getName().replace('.', '_').replace('$', '_');
        ClassDesc thisDesc = ClassDesc.of(implName);
        ClassDesc annDesc = ClassDesc.of(annType.getName());

        byte[] bytes = ClassFile.of().build(thisDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC);
            cb.withSuperclass(ConstantDescs.CD_Object);
            cb.withInterfaceSymbols(annDesc);

            cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), ClassFile.ACC_PUBLIC, cod -> {
                cod.aload(0);
                cod.invokespecial(ConstantDescs.CD_Object, "<init>", MethodTypeDesc.ofDescriptor("()V"));
                cod.return_();
            });

            cb.withMethodBody("annotationType", MethodTypeDesc.of(ConstantDescs.CD_Class),
                    ClassFile.ACC_PUBLIC, cod -> {
                        emitClassRef(cod, annType);
                        cod.areturn();
                    });

            for (java.lang.reflect.Method method : annType.getDeclaredMethods()) {
                try {
                    Object value = method.invoke(ann);
                    Class<?> returnType = method.getReturnType();
                    ClassDesc returnDesc = ClassDesc.ofDescriptor(returnType.descriptorString());
                    cb.withMethodBody(method.getName(), MethodTypeDesc.of(returnDesc),
                            ClassFile.ACC_PUBLIC, cod -> {
                                emitAnnotationValue(cod, method.getGenericReturnType(), returnType, value, baseName, classLoader);
                                cod.return_(typeKindOf(returnType));
                            });
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to generate element method for " + method, e);
                }
            }
        });

        classLoader.register(implName, bytes);
        return thisDesc;
    }

    @SuppressWarnings("rawtypes")
    private static void emitAnnotationValue(CodeBuilder cod, java.lang.reflect.Type genericType, Class<?> rawType,
            Object value, String baseName, CritterClassLoader classLoader) {
        if (rawType == String.class) {
            cod.ldc((String) value);
        } else if (rawType == boolean.class) {
            cod.loadConstant(((Boolean) value) ? 1 : 0);
        } else if (rawType == byte.class || rawType == short.class || rawType == int.class) {
            cod.loadConstant(((Number) value).intValue());
        } else if (rawType == char.class) {
            cod.loadConstant((int) (char) (Character) value);
        } else if (rawType == long.class) {
            cod.loadConstant((long) value);
        } else if (rawType == float.class) {
            cod.loadConstant((float) value);
        } else if (rawType == double.class) {
            cod.loadConstant((double) value);
        } else if (rawType == Class.class || (genericType instanceof ParameterizedType pt && pt.getRawType() == Class.class)) {
            emitClassRef(cod, (Class<?>) value);
        } else if (rawType.isEnum()) {
            Enum e = (Enum) value;
            ClassDesc enumDesc = ClassDesc.of(e.getDeclaringClass().getName());
            cod.getstatic(enumDesc, e.name(), enumDesc);
        } else if (rawType.isAnnotation()) {
            ClassDesc implDesc = generateAnnotationImpl((Annotation) value, baseName, classLoader);
            cod.new_(implDesc);
            cod.dup();
            cod.invokespecial(implDesc, "<init>", MethodTypeDesc.ofDescriptor("()V"));
        } else if (rawType.isArray()) {
            Class<?> compType = rawType.getComponentType();
            Object[] arr = (Object[]) value;
            cod.loadConstant(arr.length);
            cod.anewarray(ClassDesc.ofDescriptor(compType.descriptorString()));
            for (int i = 0; i < arr.length; i++) {
                cod.dup();
                cod.loadConstant(i);
                emitAnnotationValue(cod, compType, compType, arr[i], baseName + "_" + i, classLoader);
                cod.aastore();
            }
        } else if (genericType instanceof GenericArrayType gat) {
            java.lang.reflect.Type compGeneric = gat.getGenericComponentType();
            Class<?> compClass = (compGeneric instanceof ParameterizedType pt)
                    ? (Class<?>) pt.getRawType()
                    : (Class<?>) compGeneric;
            Object[] arr = (Object[]) value;
            cod.loadConstant(arr.length);
            cod.anewarray(ClassDesc.of(compClass.getName()));
            for (int i = 0; i < arr.length; i++) {
                cod.dup();
                cod.loadConstant(i);
                emitAnnotationValue(cod, compGeneric, compClass, arr[i], baseName + "_" + i, classLoader);
                cod.aastore();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported annotation element type: " + rawType);
        }
    }

    private static TypeKind typeKindOf(Class<?> type) {
        if (type == long.class)
            return TypeKind.LONG;
        if (type == float.class)
            return TypeKind.FLOAT;
        if (type == double.class)
            return TypeKind.DOUBLE;
        if (type.isPrimitive())
            return TypeKind.INT;
        return TypeKind.REFERENCE;
    }

    /**
     * Emits bytecode that loads a Class reference. Non-public classes use Class.forName().
     */
    public static void emitClassRef(CodeBuilder cod, Class<?> cls) {
        if (cls.isPrimitive()) {
            // Primitives are not loadable via ClassDesc.of; use the wrapper's TYPE field
            String wrapper = primitiveWrapperName(cls);
            cod.getstatic(ClassDesc.of(wrapper), "TYPE", ConstantDescs.CD_Class);
        } else if (Modifier.isPublic(cls.getModifiers())) {
            cod.loadConstant(ClassDesc.of(cls.getName()));
        } else {
            cod.ldc(cls.getName());
            cod.iconst_0();
            cod.invokestatic(
                    ClassDesc.of("java.lang.Thread"),
                    "currentThread",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Thread")));
            cod.invokevirtual(
                    ClassDesc.of("java.lang.Thread"),
                    "getContextClassLoader",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.ClassLoader")));
            cod.invokestatic(
                    ConstantDescs.CD_Class,
                    "forName",
                    MethodTypeDesc.ofDescriptor("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"));
        }
    }

    private static String primitiveWrapperName(Class<?> primitive) {
        if (primitive == boolean.class)
            return "java.lang.Boolean";
        if (primitive == byte.class)
            return "java.lang.Byte";
        if (primitive == char.class)
            return "java.lang.Character";
        if (primitive == short.class)
            return "java.lang.Short";
        if (primitive == int.class)
            return "java.lang.Integer";
        if (primitive == long.class)
            return "java.lang.Long";
        if (primitive == float.class)
            return "java.lang.Float";
        if (primitive == double.class)
            return "java.lang.Double";
        if (primitive == void.class)
            return "java.lang.Void";
        throw new IllegalArgumentException("Not a primitive: " + primitive);
    }

    /**
     * Emits bytecode that constructs a TypeData instance.
     */
    public static void emitTypeData(TypeData<?> data, CodeBuilder cod) {
        ClassDesc tdDesc = ClassDesc.of("dev.morphia.mapping.codec.pojo.TypeData");
        cod.new_(tdDesc);
        cod.dup();
        emitClassRef(cod, data.getType());
        List<TypeData<?>> params = data.getTypeParameters();
        cod.loadConstant(params.size());
        cod.anewarray(tdDesc);
        for (int i = 0; i < params.size(); i++) {
            cod.dup();
            cod.loadConstant(i);
            emitTypeData(params.get(i), cod);
            cod.aastore();
        }
        cod.invokespecial(tdDesc, "<init>",
                MethodTypeDesc.ofDescriptor("(Ljava/lang/Class;[Ldev/morphia/mapping/codec/pojo/TypeData;)V"));
    }

    /**
     * Resolves a ClassDesc to a Class using the given class loader.
     */
    public static Class<?> asClass(ClassDesc cd, ClassLoader classLoader) {
        String desc = cd.descriptorString();
        if (desc.length() == 1) {
            return switch (desc.charAt(0)) {
                case 'V' -> void.class;
                case 'Z' -> boolean.class;
                case 'C' -> char.class;
                case 'B' -> byte.class;
                case 'S' -> short.class;
                case 'I' -> int.class;
                case 'J' -> long.class;
                case 'F' -> float.class;
                case 'D' -> double.class;
                default -> throw new IllegalArgumentException("Unknown descriptor: " + desc);
            };
        }
        String className = desc.startsWith("[") ? desc.replace('/', '.') : desc.substring(1, desc.length() - 1).replace('/', '.');
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find class: " + className, e);
        }
    }

}
