package dev.morphia.mapping.codec.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

import org.bson.codecs.pojo.TypeWithTypeParameters;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;

/**
 * Holds type information about a type element
 *
 * @param <T> the underlying type being represented
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class TypeData<T> implements TypeWithTypeParameters<T> {

    private final Class<T> type;
    private final List<TypeData<?>> typeParameters = new ArrayList<>();
    private boolean array;

    /**
     * Creates a new TypeData with the concrete type and type parameters around it.
     * <p>
     * e.g., List&lt;Address&gt; would be
     *
     * <pre>
     * <code>
     * new TypeData(Address.class, TypeData.builder(List.class).build())
     * </code>
     * </pre>
     *
     * @param type the type
     */
    public TypeData(Class<T> type) {
        this.type = type;
        array = type.isArray();
    }

    /**
     * Creates a new TypeData with the concrete type and type parameters around it.
     * <p>
     * e.g., List&lt;Address&gt; would be
     *
     * <pre>
     * <code>
     * new TypeData(Address.class, TypeData.builder(List.class).build())
     * </code>
     * </pre>
     *
     * @param type           the type
     * @param typeParameters the parameters
     */
    public TypeData(Class<T> type, TypeData<?>... typeParameters) {
        this.type = type;
        this.typeParameters.addAll(asList(typeParameters));
        array = type.isArray();
    }

    /**
     * Creates a new TypeData with the concrete type and type parameters around it.
     * <p>
     * e.g., List&lt;Address&gt; would be
     *
     * <pre>
     * <code>
     * new TypeData(Address.class, TypeData.builder(List.class).build())
     * </code>
     * </pre>
     *
     * @param type           the type
     * @param typeParameters the parameters
     */
    public TypeData(Class<T> type, List<TypeData<?>> typeParameters) {
        this.type = type;
        this.typeParameters.addAll(typeParameters);
        array = type.isArray();
    }

    /**
     * Creates a new builder for ClassTypeData
     *
     * @param type the class for the type
     * @param <T>  the type
     * @return the builder
     */
    public static <T> Builder<T> builder(Class<T> type) {
        return new Builder<>(notNull("type", type));
    }

    /**
     * Creates a TypeData reflecting the given type. Type variables left in it become {@code Object}, so resolve it with
     * {@link TypeResolver#resolve(Class, Type)} first when it's declared in a generic class's hierarchy.
     *
     * @param type the type to analyze
     * @return the new TypeData information
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static TypeData<?> get(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Builder paramBuilder = TypeData.builder((Class) pType.getRawType());
            for (Type argType : TypeResolver.typeArguments(pType)) {
                paramBuilder.addTypeParameter(get(argType));
            }
            return paramBuilder.build();
        } else if (type instanceof WildcardType) {
            // the upper bound is the only type every value fits: a List<? super Integer> can hold a List<Object>'s values,
            // so its elements decode as its upper bound, Object, not as Integer
            WildcardType wildcardType = (WildcardType) type;
            return new WildCardTypeData(get(wildcardType.getUpperBounds()[0]), true);
        } else if (type instanceof TypeVariable) {
            return TypeData.builder(Object.class).build();
        } else if (type instanceof Class) {
            var typeData = new TypeData((Class) type);
            for (Type argType : TypeResolver.typeArguments(type)) {
                typeData.typeParameters.add(argType.equals(type) ? new TypeData<>((Class) argType) : get(argType));
            }
            return typeData;
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            TypeData<?> typeData = get(arrayType.getGenericComponentType());
            typeData.setArray(true);
            return typeData;
        }

        throw new UnsupportedOperationException(Sofia.unhandledTypeData(type.getClass()));
    }

    private static String nestedTypeParameters(List<TypeData<?>> typeParameters) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        int last = typeParameters.size();
        for (TypeData<?> typeParameter : typeParameters) {
            count++;
            builder.append(typeParameter.getType().getSimpleName());
            if (!typeParameter.getTypeParameters().isEmpty()) {
                builder.append(format("<%s>", nestedTypeParameters(typeParameter.getTypeParameters())));
            }
            if (count < last) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    /**
     * Creates a TypeData reflecting the type of the given field.
     *
     * @param field the field to analyze
     * @return the new TypeData information
     */
    public static TypeData<?> get(Field field) {
        return get(field.getGenericType());
    }

    /**
     * Creates a TypeData reflecting the type of the given method.
     *
     * @param method the method to analyze
     * @return the new TypeData information
     */
    public static TypeData<?> get(Method method) {
        return get(method.getGenericReturnType());
    }

    /**
     * @return true if an array
     */
    public boolean getArray() {
        return array;
    }

    /**
     * @return the class this {@code ClassTypeData} represents
     */
    @Override
    public Class<T> getType() {
        return type;
    }

    /**
     * @return the type parameters for the class
     */
    @Override
    public List<TypeData<?>> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + getTypeParameters().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeData)) {
            return false;
        }

        TypeData<?> that = (TypeData<?>) o;

        if (!getType().equals(that.getType())) {
            return false;
        }
        return getTypeParameters().equals(that.getTypeParameters());
    }

    /**
     * @return true if an array
     */
    public boolean isArray() {
        return array;
    }

    /**
     * @param array
     */
    public void setArray(boolean array) {
        this.array = array;
    }

    @Override
    public String toString() {
        String value = type.getSimpleName();
        if (!typeParameters.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ", "<", ">");
            typeParameters.forEach(t -> {
                joiner.add(t.toString());
            });
            value += joiner;
        }

        return value;
    }

    /*
     * @Override
     * public String toString() {
     * String typeParams = typeParameters.isEmpty() ? ""
     * : ", typeParameters=[" + nestedTypeParameters(typeParameters) + "]";
     * return "TypeData{"
     * + "type=" + type.getSimpleName()
     * + typeParams
     * + "}";
     * }
     */

    /**
     * Creates a new TypeData with an updated type
     *
     * @param concreteClass the new type
     * @return the new TypeData
     * @since 2.2
     */
    public TypeData<?> withType(Class<?> concreteClass) {
        return new TypeData<>(concreteClass, new ArrayList<>(typeParameters));
    }

    /**
     * A builder for TypeData
     *
     * @param <T> the main type
     */
    public static final class Builder<T> {
        private final Class<T> type;
        private final List<TypeData<?>> typeParameters = new ArrayList<>();

        private Builder(Class<T> type) {
            this.type = type;
        }

        /**
         * Adds a type parameter
         *
         * @param typeParameter the type parameter
         * @param <S>           the type of the type parameter
         * @return this
         */
        public <S> Builder<T> addTypeParameter(TypeData<S> typeParameter) {
            typeParameters.add(notNull("typeParameter", typeParameter));
            return this;
        }

        /**
         * @return the class type data
         */
        public TypeData<T> build() {
            return new TypeData<>(type, typeParameters);
        }

        @Override
        public String toString() {
            String value = type.getSimpleName();
            if (!typeParameters.isEmpty()) {
                StringJoiner joiner = new StringJoiner(", ", "<", ">");
                typeParameters.forEach(t -> {
                    joiner.add(t.toString());
                });
                value += joiner;
            }

            return value;
        }
    }
}
