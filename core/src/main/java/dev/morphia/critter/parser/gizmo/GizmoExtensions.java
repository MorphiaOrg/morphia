package dev.morphia.critter.parser.gizmo;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import dev.morphia.mapping.codec.pojo.TypeData;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

/**
 * Static utility methods bridging annotation introspection and Morphia type data with the ClassFile API.
 */
public class GizmoExtensions {

    private GizmoExtensions() {
    }

    /**
     * Emits bytecode that leaves the given annotation instance on the stack.
     * Uses the annotation builder pattern: XxxBuilder.xxxBuilder().field1(v1).build().
     */
    public static void emitAnnotationOnStack(CodeBuilder cod, java.lang.annotation.Annotation annotation) {
        Class<?> annType = annotation.annotationType();
        String className = annType.getName();
        String classPackage = className.substring(0, className.lastIndexOf('.'));
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        String builderClassName = classPackage + ".internal." + simpleName + "Builder";
        ClassDesc builderDesc = ClassDesc.of(builderClassName);
        ClassDesc annDesc = ClassDesc.of(className);
        String factoryMethod = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1) + "Builder";

        // Call builder factory: XxxBuilder.xxxBuilder()
        cod.invokestatic(builderDesc, factoryMethod, MethodTypeDesc.of(builderDesc));
        int builderSlot = cod.allocateLocal(TypeKind.REFERENCE);
        cod.astore(builderSlot);

        // For each annotation element that has a non-default value, emit setter call
        for (java.lang.reflect.Method method : annType.getDeclaredMethods()) {
            try {
                Object value = method.invoke(annotation);
                Object defaultValue = method.getDefaultValue();
                if (value == null || value.equals(defaultValue)) {
                    continue;
                }
                java.lang.reflect.Type elemType = method.getGenericReturnType();
                ClassDesc paramDesc = rawTypeDesc(elemType);

                cod.aload(builderSlot);
                emitAnnotationElementValue(cod, elemType, value);
                cod.invokevirtual(builderDesc, method.getName(), MethodTypeDesc.of(builderDesc, paramDesc));
                cod.astore(builderSlot);
            } catch (Exception e) {
                throw new RuntimeException("Failed to emit annotation element " + method.getName(), e);
            }
        }

        // Call .build()
        cod.aload(builderSlot);
        cod.invokevirtual(builderDesc, "build", MethodTypeDesc.of(annDesc));
    }

    @SuppressWarnings("unchecked")
    private static void emitAnnotationElementValue(CodeBuilder cod, java.lang.reflect.Type type, Object value) {
        if (type == String.class) {
            cod.ldc((String) value);
        } else if (type == boolean.class || type == Boolean.class) {
            cod.loadConstant(((Boolean) value) ? 1 : 0);
        } else if (type == int.class || type == Integer.class) {
            cod.loadConstant((int) value);
        } else if (type == long.class || type == Long.class) {
            cod.loadConstant((long) value);
        } else if (type == float.class || type == Float.class) {
            cod.loadConstant((float) value);
        } else if (type == double.class || type == Double.class) {
            cod.loadConstant((double) value);
        } else if (type == Class.class) {
            emitClassRef(cod, (Class<?>) value);
        } else if (type instanceof Class<?> t && t.isEnum()) {
            Enum<?> e = (Enum<?>) value;
            ClassDesc enumDesc = ClassDesc.of(e.getDeclaringClass().getName());
            cod.getstatic(enumDesc, e.name(), enumDesc);
        } else if (type instanceof Class<?> t && t.isAnnotation()) {
            emitAnnotationOnStack(cod, (java.lang.annotation.Annotation) value);
        } else if (type instanceof Class<?> t && t.isArray()) {
            Class<?> componentType = t.getComponentType();
            Object[] arr = (Object[]) value;
            emitObjectArray(cod, componentType, arr);
        } else if (type instanceof ParameterizedType pt && pt.getRawType() == Class.class) {
            emitClassRef(cod, (Class<?>) value);
        } else if (type instanceof GenericArrayType gat) {
            java.lang.reflect.Type compType = gat.getGenericComponentType();
            Class<?> compClass = (compType instanceof ParameterizedType pt)
                    ? (Class<?>) pt.getRawType()
                    : (Class<?>) compType;
            Object[] arr = (Object[]) value;
            emitObjectArray(cod, compClass, arr);
        } else {
            throw new UnsupportedOperationException("Unsupported annotation element type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private static void emitObjectArray(CodeBuilder cod, Class<?> componentType, Object[] arr) {
        cod.loadConstant(arr.length);
        cod.anewarray(ClassDesc.of(componentType.getName()));
        for (int i = 0; i < arr.length; i++) {
            cod.dup();
            cod.loadConstant(i);
            emitAnnotationElementValue(cod, componentType, arr[i]);
            cod.aastore();
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
     * Returns the raw type ClassDesc for use as a builder method parameter descriptor.
     */
    public static ClassDesc rawTypeDesc(java.lang.reflect.Type type) {
        if (type instanceof Class<?> c) {
            if (c.isPrimitive()) {
                return primitiveDesc(c);
            }
            if (c.isArray()) {
                return ClassDesc.ofDescriptor(classToDescriptor(c));
            }
            return ClassDesc.of(c.getName());
        } else if (type instanceof ParameterizedType pt) {
            return ClassDesc.of(((Class<?>) pt.getRawType()).getName());
        } else if (type instanceof GenericArrayType gat) {
            java.lang.reflect.Type comp = gat.getGenericComponentType();
            Class<?> rawComp = (comp instanceof ParameterizedType pt) ? (Class<?>) pt.getRawType() : (Class<?>) comp;
            return ClassDesc.ofDescriptor("[L" + rawComp.getName().replace('.', '/') + ";");
        } else {
            throw new UnsupportedOperationException("Unknown type: " + type);
        }
    }

    private static ClassDesc primitiveDesc(Class<?> c) {
        if (c == boolean.class)
            return ConstantDescs.CD_boolean;
        if (c == byte.class)
            return ConstantDescs.CD_byte;
        if (c == char.class)
            return ConstantDescs.CD_char;
        if (c == short.class)
            return ConstantDescs.CD_short;
        if (c == int.class)
            return ConstantDescs.CD_int;
        if (c == long.class)
            return ConstantDescs.CD_long;
        if (c == float.class)
            return ConstantDescs.CD_float;
        if (c == double.class)
            return ConstantDescs.CD_double;
        throw new IllegalArgumentException("Not a primitive: " + c);
    }

    private static String classToDescriptor(Class<?> c) {
        if (c.isArray()) {
            return "[" + classToDescriptor(c.getComponentType());
        }
        if (c.isPrimitive()) {
            if (c == boolean.class)
                return "Z";
            if (c == byte.class)
                return "B";
            if (c == char.class)
                return "C";
            if (c == short.class)
                return "S";
            if (c == int.class)
                return "I";
            if (c == long.class)
                return "J";
            if (c == float.class)
                return "F";
            if (c == double.class)
                return "D";
            if (c == void.class)
                return "V";
        }
        return "L" + c.getName().replace('.', '/') + ";";
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

    /**
     * Returns the generic return type of the named annotation element method.
     */
    public static java.lang.reflect.Type attributeType(Class<?> type, String name) {
        try {
            return type.getDeclaredMethod(name).getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot find annotation element '%s' in %s".formatted(name, type.getName()), e);
        }
    }

    /**
     * Creates a TypeData from the given ClassDesc with explicit type parameters.
     */
    public static TypeData<?> typeDataFromDesc(ClassDesc desc, ClassLoader classLoader, List<TypeData<?>> typeParameters) {
        return new TypeData<>(asClass(desc, classLoader), typeParameters);
    }

    /**
     * Creates a TypeData from the given ClassDesc with no type parameters.
     */
    public static TypeData<?> typeDataFromDesc(ClassDesc desc, ClassLoader classLoader) {
        return typeDataFromDesc(desc, classLoader, List.of());
    }
}
