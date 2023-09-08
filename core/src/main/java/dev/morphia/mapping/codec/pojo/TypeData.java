package dev.morphia.mapping.codec.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

import org.bson.codecs.pojo.TypeWithTypeParameters;

import static java.lang.String.format;
import static org.bson.assertions.Assertions.notNull;

/**
 * Holds type information about a type element
 *
 * @param <T> the underlying type being represented
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
     * @param type           the type
     */
    public TypeData(Class<T> type) {
        this.type = type;
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static TypeData<?> get(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            TypeParameters parameters = TypeParameters.of(pType);
            Builder paramBuilder = TypeData.builder((Class) pType.getRawType());
            for (Type argType : parameters) {
                paramBuilder.addTypeParameter(get(argType));
            }
            return paramBuilder.build();
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            Type[] bounds = upperBounds != null
                    ? upperBounds
                    : wildcardType.getLowerBounds();
            return new WildCardTypeData(get(bounds[0]), upperBounds != null);
        } else if (type instanceof TypeVariable) {
            return TypeData.builder(Object.class).build();
        } else if (type instanceof Class) {
            var typeData = new TypeData((Class) type);
            for (Type argType : TypeParameters.of(type)) {
                typeData.typeParameters.add(argType.equals(type) ? type : get(argType));
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

    static class TypeParameters implements Iterable<Type> {
        final List<Param> params = new ArrayList<>();

        public static TypeParameters of(Type type) {
            if (type instanceof Class<?>) {
                Class<?> klass = (Class<?>) type;
                TypeVariable<? extends Class<?>>[] parameters = ((Class<?>) type).getTypeParameters();
                TypeParameters params = new TypeParameters();
                for (TypeVariable<? extends Class<?>> parameter : parameters) {
                    params.add(new Param(parameter.getName(), Object.class));
                }
                Type superclass = klass.getGenericSuperclass();
                if (!klass.isEnum() && superclass instanceof ParameterizedType) {
                    return of((ParameterizedType) superclass, params);
                }
                return params;
            } else {
                throw new UnsupportedOperationException("Unsupported type passed: " + type);
            }
        }

        public static TypeParameters of(ParameterizedType type) {
            return of(type, new TypeParameters());
        }

        private static TypeParameters of(ParameterizedType type, TypeParameters subtypeParams) {
            TypeParameters params = new TypeParameters();
            Type[] typeArguments = type.getActualTypeArguments();
            TypeVariable<?>[] typeParameters = ((Class<?>) type.getRawType()).getTypeParameters();
            int index = 0;
            for (int i = 0; i < typeParameters.length; i++) {
                Type typeArgument = typeArguments[i];
                if (typeArgument instanceof TypeVariable && index < subtypeParams.params.size()) {
                    typeArgument = subtypeParams.params.get(index++).type;
                }
                params.add(new Param(typeParameters[i].getName(), typeArgument));
            }
            Type genericSuperclass = ((Class<?>) type.getRawType()).getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                params = TypeParameters.of((ParameterizedType) genericSuperclass, params);
            }
            return params;
        }

        @Override
        public void forEach(Consumer<? super Type> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Spliterator<Type> spliterator() {
            return Iterable.super.spliterator();
        }

        @Override
        public Iterator<Type> iterator() {
            return params.stream().map(p -> p.type)
                    .iterator();
        }

        private void add(Param param) {
            params.add(param);
        }

        @Override
        public String toString() {
            return format("TypeParameters{%s}", params);
        }

        private static class Param {
            private String name;
            private Type type;

            public Param(String name, Type type) {
                this.name = name;
                this.type = type;
            }

            @Override
            public String toString() {
                return format("Param{name='%s', type=%s}", name, type.getTypeName());
            }
        }
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
        return newInstance(method.getGenericReturnType());
    }

    /**
     * Creates a TypeData reflecting the given generic type and class.
     *
     * @param genericType the type to analyze
     * @param <T>         the type of the new TypeData instance
     * @return the new TypeData information
     */
    public static <T> TypeData<T> newInstance(Type genericType) {
        /*
         * TypeData.Builder<T> builder = TypeData.builder(clazz);
         * if (genericType instanceof ParameterizedType) {
         * ParameterizedType pType = (ParameterizedType) genericType;
         * for (Type argType : pType.getActualTypeArguments()) {
         * builder.addTypeParameter(getTypeData(argType));
         * }
         * }
         * return builder.build();
         */
        return (TypeData<T>) get(genericType);
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
            return new TypeData<>(type, Collections.unmodifiableList(typeParameters));
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
