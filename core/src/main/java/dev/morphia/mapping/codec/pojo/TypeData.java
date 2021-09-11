package dev.morphia.mapping.codec.pojo;

import dev.morphia.sofia.Sofia;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import static java.lang.String.format;
import static org.bson.assertions.Assertions.notNull;

/**
 * Holds type information about a type element
 *
 * @param <T> the underlying type being represented
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class TypeData<T> implements TypeWithTypeParameters<T> {

    private final Class<T> type;
    private final List<TypeData<?>> typeParameters;
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
     * @param typeParameters the parameters
     */
    public TypeData(Class<T> type, List<TypeData<?>> typeParameters) {
        this.type = type;
        this.typeParameters = typeParameters;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TypeData<?> getTypeData(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Builder paramBuilder = TypeData.builder((Class) pType.getRawType());
            for (Type argType : pType.getActualTypeArguments()) {
                paramBuilder.addTypeParameter(getTypeData(argType));
            }
            return paramBuilder.build();
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            Type[] bounds = upperBounds != null
                            ? upperBounds
                            : wildcardType.getLowerBounds();
            return WildCardTypeData.builder(getTypeData(bounds[0]), upperBounds != null).build();
        } else if (type instanceof TypeVariable) {
            return TypeData.builder(Object.class).build();
        } else if (type instanceof Class) {
            Builder builder = TypeData.builder((Class) type);
            for (TypeVariable typeParameter : builder.type.getTypeParameters()) {
                builder.addTypeParameter(getTypeData(typeParameter));
            }
            return builder.build();
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            TypeData<?> typeData = getTypeData(arrayType.getGenericComponentType());
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
    public static TypeData<?> newInstance(Field field) {
        return newInstance(field.getGenericType());
    }

    /**
     * Creates a TypeData reflecting the type of the given method.
     *
     * @param method the method to analyze
     * @return the new TypeData information
     */
    public static TypeData<?> newInstance(Method method) {
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
          TypeData.Builder<T> builder = TypeData.builder(clazz);
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            for (Type argType : pType.getActualTypeArguments()) {
                builder.addTypeParameter(getTypeData(argType));
            }
        }
        return builder.build();
         */
        return (TypeData<T>) getTypeData(genericType);
    }

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

    public boolean isArray() {
        return array;
    }

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
    @Override
    public String toString() {
        String typeParams = typeParameters.isEmpty() ? ""
                                                     : ", typeParameters=[" + nestedTypeParameters(typeParameters) + "]";
        return "TypeData{"
               + "type=" + type.getSimpleName()
               + typeParams
               + "}";
    }
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
