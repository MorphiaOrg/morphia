package dev.morphia.critter.parser.generator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.morphia.mapping.codec.pojo.TypeData;

import io.github.dmlloyd.classfile.AnnotationElement;
import io.github.dmlloyd.classfile.AnnotationValue;
import io.github.dmlloyd.classfile.ClassBuilder;
import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
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
     * Reads and parses the class file for the given type. Returns {@code null} if the class file
     * resource cannot be found; throws {@link RuntimeException} on I/O or parse failure.
     */
    public static ClassModel readClassModel(Class<?> type) {
        String resourceName = "%s.class".formatted(type.getName().replace('.', '/'));
        try (InputStream inputStream = safeClassLoader(type).getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return null;
            }
            return ClassFile.of().parse(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class %s".formatted(type.getName()), e);
        }
    }

    /**
     * Converts a runtime annotation instance into a ClassFile API annotation descriptor, suitable
     * for use in {@link io.github.dmlloyd.classfile.attribute.RuntimeVisibleAnnotationsAttribute}.
     * All element values are captured at generation time.
     */
    public static io.github.dmlloyd.classfile.Annotation toClassfileAnnotation(Annotation ann) {
        Class<?> annType = ann.annotationType();
        List<AnnotationElement> elements = new ArrayList<>();
        for (java.lang.reflect.Method method : annType.getDeclaredMethods()) {
            try {
                Object value = method.invoke(ann);
                AnnotationValue av = toAnnotationValue(method.getGenericReturnType(), method.getReturnType(), value);
                elements.add(AnnotationElement.of(method.getName(), av));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to read annotation element " + method, e);
            }
        }
        return io.github.dmlloyd.classfile.Annotation.of(ClassDesc.of(annType.getName()), elements);
    }

    @SuppressWarnings("rawtypes")
    private static AnnotationValue toAnnotationValue(java.lang.reflect.Type genericType, Class<?> rawType, Object value) {
        if (rawType == String.class)
            return AnnotationValue.ofString((String) value);
        if (rawType == boolean.class)
            return AnnotationValue.ofBoolean((Boolean) value);
        if (rawType == byte.class)
            return AnnotationValue.ofByte((Byte) value);
        if (rawType == char.class)
            return AnnotationValue.ofChar((Character) value);
        if (rawType == short.class)
            return AnnotationValue.ofShort((Short) value);
        if (rawType == int.class)
            return AnnotationValue.ofInt((Integer) value);
        if (rawType == long.class)
            return AnnotationValue.ofLong((Long) value);
        if (rawType == float.class)
            return AnnotationValue.ofFloat((Float) value);
        if (rawType == double.class)
            return AnnotationValue.ofDouble((Double) value);
        if (rawType == Class.class || (genericType instanceof ParameterizedType pt && pt.getRawType() == Class.class)) {
            return AnnotationValue.ofClass(ClassDesc.ofDescriptor(((Class<?>) value).descriptorString()));
        }
        if (rawType.isEnum()) {
            Enum e = (Enum) value;
            return AnnotationValue.ofEnum(ClassDesc.of(e.getDeclaringClass().getName()), e.name());
        }
        if (rawType.isAnnotation()) {
            return AnnotationValue.ofAnnotation(toClassfileAnnotation((Annotation) value));
        }
        if (rawType.isArray()) {
            Class<?> compType = rawType.getComponentType();
            int len = Array.getLength(value);
            List<AnnotationValue> values = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                values.add(toAnnotationValue(compType, compType, Array.get(value, i)));
            }
            return AnnotationValue.ofArray(values);
        }
        if (genericType instanceof GenericArrayType gat) {
            java.lang.reflect.Type compGeneric = gat.getGenericComponentType();
            Class<?> compClass = (compGeneric instanceof ParameterizedType pt)
                    ? (Class<?>) pt.getRawType()
                    : (Class<?>) compGeneric;
            Object[] arr = (Object[]) value;
            List<AnnotationValue> values = new ArrayList<>();
            for (Object elem : arr) {
                values.add(toAnnotationValue(compGeneric, compClass, elem));
            }
            return AnnotationValue.ofArray(values);
        }
        throw new UnsupportedOperationException("Unsupported annotation element type: " + rawType);
    }

    /**
     * Emits bytecode that calls the generated AnnotationBuilder for a Morphia annotation,
     * leaving the built annotation instance on the operand stack. Values are hardcoded as
     * bytecode constants — no runtime reflection.
     *
     * <p>
     * Assumes the annotation type lives under {@code dev.morphia.annotations} and that its
     * builder follows the {@code XxxBuilder.xxxBuilder()...build()} convention.
     */
    public static void emitAnnotationViaBuilder(CodeBuilder cod, Annotation ann) {
        Class<?> annType = ann.annotationType();
        String className = annType.getName();
        int lastDot = className.lastIndexOf('.');
        String simpleName = className.substring(lastDot + 1);
        String builderClassName = className.substring(0, lastDot) + ".internal." + simpleName + "Builder";
        ClassDesc builderDesc = ClassDesc.of(builderClassName);
        ClassDesc annDesc = ClassDesc.of(className);
        String factoryMethod = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1) + "Builder";

        cod.invokestatic(builderDesc, factoryMethod, MethodTypeDesc.of(builderDesc));

        for (java.lang.reflect.Method method : annType.getDeclaredMethods()) {
            try {
                Object value = method.invoke(ann);
                Object defaultValue = method.getDefaultValue();
                if (value == null || java.util.Objects.deepEquals(value, defaultValue)) {
                    continue;
                }
                Class<?> rawType = method.getReturnType();
                ClassDesc paramDesc = ClassDesc.ofDescriptor(rawType.descriptorString());
                emitBuilderElementValue(cod, method.getGenericReturnType(), rawType, value);
                cod.invokevirtual(builderDesc, method.getName(), MethodTypeDesc.of(builderDesc, paramDesc));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to emit builder setter for " + method, e);
            }
        }

        cod.invokevirtual(builderDesc, "build", MethodTypeDesc.of(annDesc));
    }

    @SuppressWarnings("rawtypes")
    private static void emitBuilderElementValue(CodeBuilder cod, java.lang.reflect.Type genericType,
            Class<?> rawType, Object value) {
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
            emitAnnotationViaBuilder(cod, (Annotation) value);
        } else if (rawType.isArray()) {
            Class<?> compType = rawType.getComponentType();
            int len = Array.getLength(value);
            cod.loadConstant(len);
            if (compType.isPrimitive()) {
                TypeKind tk = TypeKind.fromDescriptor(compType.descriptorString());
                cod.newarray(tk);
                for (int i = 0; i < len; i++) {
                    cod.dup();
                    cod.loadConstant(i);
                    emitBuilderElementValue(cod, compType, compType, Array.get(value, i));
                    cod.arrayStore(tk);
                }
            } else {
                cod.anewarray(ClassDesc.ofDescriptor(compType.descriptorString()));
                for (int i = 0; i < len; i++) {
                    cod.dup();
                    cod.loadConstant(i);
                    emitBuilderElementValue(cod, compType, compType, Array.get(value, i));
                    cod.aastore();
                }
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
                emitBuilderElementValue(cod, compGeneric, compClass, arr[i]);
                cod.aastore();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported annotation element type: " + rawType);
        }
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
            cod.loadConstant(ClassDesc.ofDescriptor(cls.descriptorString()));
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
        if (primitive == void.class) {
            return "java.lang.Void";
        }
        String wrapper = PRIMITIVE_TO_WRAPPER.get(primitive.getName());
        if (wrapper == null) {
            throw new IllegalArgumentException("Not a primitive: " + primitive);
        }
        return wrapper;
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
