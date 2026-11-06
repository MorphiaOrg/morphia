package dev.morphia.mapping.codec.pojo;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Resolves generic types as seen from a class: each type variable its supertypes declare is bound, by identity rather
 * than by name or position, to the type the hierarchy gives it.
 *
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
@MorphiaInternal
public final class TypeResolver {
    private static final ClassValue<Map<TypeVariable<?>, Type>> BINDINGS = new ClassValue<>() {
        @Override
        protected Map<TypeVariable<?>, Type> computeValue(Class<?> type) {
            Map<TypeVariable<?>, Type> bindings = new HashMap<>();
            bind(bindings, type.getGenericSuperclass());
            for (Type anInterface : type.getGenericInterfaces()) {
                bind(bindings, anInterface);
            }
            return bindings.isEmpty() ? Map.of() : Collections.unmodifiableMap(bindings);
        }
    };

    private TypeResolver() {
    }

    /**
     * Resolves a type declared somewhere in a class's hierarchy as that class sees it. Type variables the hierarchy
     * leaves unbound, such as the class's own, are left in place.
     *
     * @param context the class the type is seen from
     * @param type    the type to resolve
     * @return the resolved type
     */
    public static Type resolve(Class<?> context, Type type) {
        return substitute(type, BINDINGS.get(context));
    }

    /**
     * Lists the type arguments {@link TypeData} reports for a type: those of the furthest superclass reached through
     * parameterized superclasses, e.g. the key and value types of a {@code HashMap} subclass, or the type's own when its
     * superclass isn't parameterized.
     *
     * @param type a class or parameterized type
     * @return the type arguments, with any the type leaves unbound as type variables
     */
    static List<Type> typeArguments(Type type) {
        Class<?> raw;
        Map<TypeVariable<?>, Type> arguments;
        if (type instanceof ParameterizedType parameterized) {
            raw = (Class<?>) parameterized.getRawType();
            arguments = arguments(raw, parameterized);
        } else {
            raw = (Class<?>) type;
            arguments = Map.of();
        }
        Class<?> top = raw;
        while (!top.isEnum() && top.getGenericSuperclass() instanceof ParameterizedType) {
            top = top.getSuperclass();
        }
        Map<TypeVariable<?>, Type> inherited = BINDINGS.get(raw);
        List<Type> list = new ArrayList<>();
        for (TypeVariable<?> variable : top.getTypeParameters()) {
            list.add(substitute(inherited.getOrDefault(variable, variable), arguments));
        }
        return list;
    }

    private static void bind(Map<TypeVariable<?>, Type> bindings, @Nullable Type supertype) {
        if (supertype instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            Map<TypeVariable<?>, Type> arguments = arguments(raw, parameterized);
            bindings.putAll(arguments);
            for (Entry<TypeVariable<?>, Type> entry : BINDINGS.get(raw).entrySet()) {
                bindings.put(entry.getKey(), substitute(entry.getValue(), arguments));
            }
        } else if (supertype instanceof Class<?> type) {
            bindings.putAll(BINDINGS.get(type));
        }
    }

    private static Map<TypeVariable<?>, Type> arguments(Class<?> raw, ParameterizedType type) {
        TypeVariable<?>[] variables = raw.getTypeParameters();
        Type[] arguments = type.getActualTypeArguments();
        Map<TypeVariable<?>, Type> map = new HashMap<>();
        for (int i = 0; i < variables.length; i++) {
            map.put(variables[i], arguments[i]);
        }
        return map;
    }

    private static Type substitute(Type type, Map<TypeVariable<?>, Type> bindings) {
        if (bindings.isEmpty() || type instanceof Class) {
            return type;
        } else if (type instanceof TypeVariable<?> variable) {
            return bindings.getOrDefault(variable, variable);
        } else if (type instanceof ParameterizedType parameterized) {
            Type[] arguments = substitute(parameterized.getActualTypeArguments(), bindings);
            return arguments == null
                    ? type
                    : new ResolvedParameterizedType((Class<?>) parameterized.getRawType(), parameterized.getOwnerType(), arguments);
        } else if (type instanceof GenericArrayType array) {
            Type component = substitute(array.getGenericComponentType(), bindings);
            if (component == array.getGenericComponentType()) {
                return type;
            }
            return component instanceof Class<?> c
                    ? Array.newInstance(c, 0).getClass()
                    : new ResolvedGenericArrayType(component);
        } else if (type instanceof WildcardType wildcard) {
            Type[] upper = substitute(wildcard.getUpperBounds(), bindings);
            Type[] lower = substitute(wildcard.getLowerBounds(), bindings);
            return upper == null && lower == null
                    ? type
                    : new ResolvedWildcardType(upper != null ? upper : wildcard.getUpperBounds(),
                            lower != null ? lower : wildcard.getLowerBounds());
        }
        return type;
    }

    /**
     * @return the substituted types, or null when none changed
     */
    @Nullable
    private static Type[] substitute(Type[] types, Map<TypeVariable<?>, Type> bindings) {
        Type[] substituted = null;
        for (int i = 0; i < types.length; i++) {
            Type type = substitute(types[i], bindings);
            if (type != types[i]) {
                if (substituted == null) {
                    substituted = types.clone();
                }
                substituted[i] = type;
            }
        }
        return substituted;
    }

    private record ResolvedParameterizedType(Class<?> rawType, @Nullable Type ownerType, Type[] arguments)
            implements
                ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        @Nullable
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ParameterizedType that
                    && rawType.equals(that.getRawType())
                    && Objects.equals(ownerType, that.getOwnerType())
                    && Arrays.equals(arguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ Objects.hashCode(ownerType) ^ rawType.hashCode();
        }

        @Override
        public String toString() {
            return getTypeName();
        }

        @Override
        public String getTypeName() {
            StringBuilder name = new StringBuilder(rawType.getTypeName()).append('<');
            for (int i = 0; i < arguments.length; i++) {
                name.append(i == 0 ? "" : ", ").append(arguments[i].getTypeName());
            }
            return name.append('>').toString();
        }
    }

    private record ResolvedGenericArrayType(Type component) implements GenericArrayType {
        @Override
        public Type getGenericComponentType() {
            return component;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GenericArrayType that && component.equals(that.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return component.hashCode();
        }

        @Override
        public String toString() {
            return getTypeName();
        }

        @Override
        public String getTypeName() {
            return component.getTypeName() + "[]";
        }
    }

    private record ResolvedWildcardType(Type[] upper, Type[] lower) implements WildcardType {
        @Override
        public Type[] getUpperBounds() {
            return upper.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lower.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof WildcardType that
                    && Arrays.equals(upper, that.getUpperBounds())
                    && Arrays.equals(lower, that.getLowerBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(upper) ^ Arrays.hashCode(lower);
        }

        @Override
        public String toString() {
            return getTypeName();
        }

        @Override
        public String getTypeName() {
            return lower.length != 0
                    ? "? super " + lower[0].getTypeName()
                    : upper.length == 0 || upper[0] == Object.class ? "?" : "? extends " + upper[0].getTypeName();
        }
    }
}
