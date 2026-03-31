package dev.morphia.critter.parser.gizmo;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import dev.morphia.critter.parser.ExtensionFunctions;
import dev.morphia.critter.parser.Generators;
import dev.morphia.mapping.codec.pojo.TypeData;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

/**
 * Static utility methods that bridge ASM annotation nodes and Morphia type data with the Gizmo bytecode generation API.
 */
public class GizmoExtensions {

    /** @hidden */
    private GizmoExtensions() {
    }

    /**
     * Emits Gizmo bytecode that instantiates the Morphia annotation represented by the given ASM annotation node.
     *
     * @param annotationNode the ASM annotation node to convert
     * @param creator        the Gizmo method creator in which the bytecode is emitted
     * @return a result handle for the constructed annotation instance
     */
    public static ResultHandle annotationBuilder(AnnotationNode annotationNode, MethodCreator creator) {
        Type type = Type.getType(annotationNode.desc);
        String classPackage = type.getClassName().substring(0, type.getClassName().lastIndexOf('.'));
        String className = type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1);
        Type builderType = Type.getType("L%s.internal.%sBuilder;".formatted(classPackage, className).replace('.', '/'));
        MethodDescriptor builder = ofMethod(
                builderType.getClassName(),
                ExtensionFunctions.methodCase(className) + "Builder",
                builderType.getClassName());
        ResultHandle local = creator.invokeStaticMethod(builder);

        setBuilderValues(annotationNode, creator, local);

        return creator.invokeVirtualMethod(ofMethod(builderType.getClassName(), "build", type.getClassName()), local);
    }

    /**
     * Populates the annotation builder with values from the ASM annotation node.
     *
     * @param annotationNode the ASM annotation node whose values should be applied
     * @param creator        the Gizmo method creator in which the bytecode is emitted
     * @param local          the result handle referencing the annotation builder instance
     */
    public static void setBuilderValues(AnnotationNode annotationNode, MethodCreator creator, ResultHandle local) {
        dev.morphia.annotations.internal.AnnotationNodeExtensions.INSTANCE.setBuilderValues(annotationNode, creator, local);
    }

    /**
     * Emits Gizmo bytecode that constructs a {@link TypeData} instance matching the given type data.
     *
     * @param data          the type data to emit
     * @param methodCreator the Gizmo method creator in which the bytecode is emitted
     * @return a result handle for the constructed {@code TypeData} instance
     */
    public static ResultHandle emitTypeData(TypeData<?> data, MethodCreator methodCreator) {
        ResultHandle array = methodCreator.newArray(TypeData.class, data.getTypeParameters().size());
        List<TypeData<?>> typeParameters = data.getTypeParameters();
        for (int index = 0; index < typeParameters.size(); index++) {
            methodCreator.writeArrayValue(array, index, emitTypeData(typeParameters.get(index), methodCreator));
        }
        List<ResultHandle> list = new ArrayList<>();
        list.add(methodCreator.loadClass(data.getType()));
        list.add(array);
        MethodDescriptor descriptor = MethodDescriptor.ofConstructor(
                TypeData.class,
                Class.class,
                "[" + Type.getType(TypeData.class).getDescriptor());
        return methodCreator.newInstance(descriptor, list.toArray(new ResultHandle[0]));
    }

    /**
     * Returns the ASM type descriptor for the raw (erased) form of the given {@link java.lang.reflect.Type}.
     *
     * @param type the type to erase
     * @return the ASM descriptor string for the raw type
     */
    public static String rawType(java.lang.reflect.Type type) {
        if (type instanceof GenericArrayType arrayType) {
            ParameterizedType type1 = (ParameterizedType) arrayType.getGenericComponentType();
            return Type.getType("[" + Type.getType((Class<?>) type1.getRawType()).getDescriptor()).getDescriptor();
        } else if (type instanceof ParameterizedType paramType) {
            return Type.getType((Class<?>) paramType.getRawType()).getDescriptor();
        } else {
            return Type.getType((Class<?>) type).getDescriptor();
        }
    }

    /**
     * Returns the generic return type of the named annotation element method.
     *
     * @param type the annotation class containing the element
     * @param name the name of the annotation element
     * @return the generic return type of the element method
     * @throws RuntimeException if the element method cannot be found
     */
    public static java.lang.reflect.Type attributeType(Class<?> type, String name) {
        try {
            return type.getDeclaredMethod(name).getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot find annotation element '%s' in %s".formatted(name, type.getName()), e);
        }
    }

    /**
     * Emits Gizmo bytecode that loads the given value as the specified {@link java.lang.reflect.Type}.
     *
     * @param creator the Gizmo method creator in which the bytecode is emitted
     * @param type    the target type
     * @param value   the value to load
     * @return a result handle for the loaded value
     */
    @SuppressWarnings("unchecked")
    public static ResultHandle load(MethodCreator creator, java.lang.reflect.Type type, Object value) {
        if (type instanceof Class<?> classType) {
            return load(creator, classType, value);
        } else if (type instanceof ParameterizedType paramType) {
            return load(creator, (Class<?>) paramType.getRawType(), value);
        } else if (type instanceof GenericArrayType arrayType) {
            String componentTypeName;
            if (arrayType.getGenericComponentType() instanceof ParameterizedType pType) {
                componentTypeName = ((Class<?>) pType.getRawType()).getName();
            } else {
                componentTypeName = ((Class<?>) arrayType.getGenericComponentType()).getName();
            }
            List<?> valueList = (List<?>) value;
            ResultHandle newArray = creator.newArray(componentTypeName, valueList.size());
            for (int index = 0; index < valueList.size(); index++) {
                Object element = valueList.get(index);
                creator.writeArrayValue(newArray, index, load(creator, element.getClass(), element));
            }
            return newArray;
        } else {
            throw new UnsupportedOperationException("Unknown type: %s".formatted(type));
        }
    }

    /**
     * Emits Gizmo bytecode that loads the given value as the specified class type.
     *
     * @param creator the Gizmo method creator in which the bytecode is emitted
     * @param type    the target class type
     * @param value   the value to load
     * @return a result handle for the loaded value
     */
    @SuppressWarnings("unchecked")
    public static ResultHandle load(MethodCreator creator, Class<?> type, Object value) {
        if (type == String.class) {
            return creator.load((String) value);
        } else if (type == int.class || type == Integer.class) {
            return creator.load((int) value);
        } else if (type == long.class || type == Long.class) {
            return creator.load((long) value);
        } else if (type == boolean.class || type == Boolean.class) {
            return creator.load((boolean) value);
        } else if (type == AnnotationNode.class) {
            return annotationBuilder((AnnotationNode) value, creator);
        } else if (type.isAnnotation()) {
            return annotationBuilder((AnnotationNode) value, creator);
        } else if (type.isArray()) {
            List<?> valueList = (List<?>) value;
            ResultHandle newArray = creator.newArray(type.getComponentType(), valueList.size());
            for (int index = 0; index < valueList.size(); index++) {
                Object element = valueList.get(index);
                creator.writeArrayValue(newArray, index, load(creator, element.getClass(), element));
            }
            return newArray;
        } else if (type.isEnum()) {
            return creator.readStaticField(FieldDescriptor.of(type, ((String[]) value)[1], type));
        } else if (value instanceof Type asmType) {
            return creator.loadClass(asmType.getClassName());
        } else {
            throw new UnsupportedOperationException("%s is not yet supported".formatted(type));
        }
    }

    /**
     * Creates a {@link TypeData} from the given ASM type with explicit type parameters.
     *
     * @param type           the ASM type to convert
     * @param classLoader    the class loader used to resolve the type
     * @param typeParameters the list of generic type parameters
     * @return a {@code TypeData} representing the type with its parameters
     */
    public static TypeData<?> typeDataFromType(Type type, ClassLoader classLoader, List<TypeData<?>> typeParameters) {
        return new TypeData<>(Generators.asClass(type, classLoader), typeParameters);
    }

    /**
     * Creates a {@link TypeData} from the given ASM type with no type parameters.
     *
     * @param type        the ASM type to convert
     * @param classLoader the class loader used to resolve the type
     * @return a {@code TypeData} representing the raw type
     */
    public static TypeData<?> typeDataFromType(Type type, ClassLoader classLoader) {
        return typeDataFromType(type, classLoader, List.of());
    }
}
