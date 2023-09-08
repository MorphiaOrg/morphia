package dev.morphia.mapping.conventions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.sofia.Sofia;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@MorphiaInternal
public class MethodDiscovery implements MorphiaConvention {
    private EntityModelBuilder entityModelBuilder;

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void apply(Mapper mapper, EntityModelBuilder builder) {
        if (builder.propertyModels().isEmpty()) {
            this.entityModelBuilder = builder;

            Set<Class<?>> hierarchy = new LinkedHashSet<>(Set.of(builder.type()));
            hierarchy.addAll(builder.classHierarchy());

            Set<Methods> properties = new LinkedHashSet<>();

            for (Class<?> type : hierarchy) {
                properties.addAll(processMethods(type));
            }

            addProperties(builder, properties);
        }
    }

    private List<Methods> processMethods(Class<?> type) {
        return stream(type.getDeclaredMethods())
                .filter(MethodDiscovery::isGetterSetter)
                .filter(m -> !m.isSynthetic()) // overloaded parent methods are synthetic on the child types
                .collect(Collectors.groupingBy(this::stripPrefix))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() == 2)
                .map(entry -> new Methods(entry.getKey(), type, entry.getValue()))
                .collect(toList());

    }

    private void addProperties(EntityModelBuilder builder, Set<Methods> properties) {
        for (Methods methods : properties) {
            TypeData<?> typeData = entityModelBuilder.getTypeData(methods.type, TypeData.get(methods.getter),
                    methods.getter.getGenericReturnType());

            entityModelBuilder.addProperty()
                    .name(methods.property)
                    .accessor(new MethodAccessor(getTargetMethod(builder, methods.getter),
                            getTargetMethod(builder, methods.setter)))
                    .annotations(discoverAnnotations(methods.getter, methods.setter))
                    .typeData(typeData)
                    .discoverMappedName();
        }
    }

    private static class Methods {
        private final Method getter;
        private final Method setter;
        private final String property;
        private final Class<?> type;

        Methods(String property, Class<?> type, List<Method> methods) {
            this.property = property;
            this.type = type;
            List<Method> collect = methods.stream().sorted(Comparator.comparing(Method::getName))
                    .collect(toList());
            getter = collect.get(0);
            setter = collect.get(1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Methods)) {
                return false;
            }
            Methods methods = (Methods) o;
            return property.equals(methods.property);
        }

        @Override
        public int hashCode() {
            return Objects.hash(property);
        }
    }

    @NonNull
    private String stripPrefix(Method m) {
        return m.getName().startsWith("get")
                || m.getName().startsWith("set")
                        ? stripPrefix(m, 3)
                        : stripPrefix(m, 2);
    }

    private static boolean isGetterSetter(Method m) {
        return m.getName().startsWith("get")
                || m.getName().startsWith("set")
                || m.getName().startsWith("is");
    }

    private List<Annotation> discoverAnnotations(Method getter, Method setter) {
        return Stream.of(getter, setter)
                .flatMap(m -> stream(m.getDeclaredAnnotations()))
                .collect(toList());
    }

    @NonNull
    private Method getTargetMethod(EntityModelBuilder builder, @NonNull Method method) {
        try {
            if (builder.type().equals(builder.targetType())) {
                return method;
            }
            return builder.targetType().getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (ReflectiveOperationException e) {
            throw new MappingException(Sofia.mismatchedMethodOnExternalType(method.getName(),
                    method.getParameterTypes(), builder.type().getName(), builder.targetType().getName()));
        }
    }

    private String stripPrefix(Method method, int size) {
        String name = method.getName().substring(size);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        return name;
    }
}
