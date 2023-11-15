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
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.sofia.Sofia;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static dev.morphia.mapping.conventions.FieldDiscovery.discoverMappedName;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@MorphiaInternal
public class MethodDiscovery implements MorphiaConvention {

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void apply(Mapper mapper, EntityModel model) {
        if (model.getProperties().isEmpty()) {

            Set<Class<?>> hierarchy = new LinkedHashSet<>(Set.of(model.getType()));
            hierarchy.addAll(model.classHierarchy());

            Set<Methods> properties = new LinkedHashSet<>();

            for (Class<?> type : hierarchy) {
                properties.addAll(processMethods(type));
            }

            addProperties(mapper, model, properties);
        }
    }

    private List<Methods> processMethods(Class<?> type) {
        return stream(type.getDeclaredMethods())
                .filter(method -> !isStatic(method.getModifiers()))
                .filter(m -> !m.isSynthetic()) // overloaded parent methods are synthetic on the child types
                .filter(MethodDiscovery::isGetterSetter)
                .collect(Collectors.groupingBy(this::stripPrefix))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() == 2)
                .map(entry -> new Methods(entry.getKey(), type, entry.getValue()))
                .collect(toList());

    }

    private void addProperties(Mapper mapper, EntityModel model, Set<Methods> properties) {
        for (Methods methods : properties) {
            TypeData<?> typeData = model.getTypeData(methods.type, TypeData.get(methods.getter),
                    methods.getter.getGenericReturnType());

            ExternalEntity externalEntity = model.getAnnotation(ExternalEntity.class);
            Class<?> target = externalEntity != null ? externalEntity.target() : null;

            PropertyModel propertyModel = new PropertyModel(model);

            model.addProperty(propertyModel
                    .name(methods.property)
                    .typeData(typeData)
                    .annotations(discoverAnnotations(methods.getter, methods.setter))
                    .accessor(new MethodAccessor(getTargetMethod(model, target, methods.getter),
                            getTargetMethod(model, target, methods.setter)))
                    .mappedName(discoverMappedName(mapper, propertyModel)));
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
    private Method getTargetMethod(EntityModel model, @Nullable Class<?> target, @NonNull Method method) {
        try {
            return target != null
                    ? target.getDeclaredMethod(method.getName(), method.getParameterTypes())
                    : method;
        } catch (ReflectiveOperationException e) {
            throw new MappingException(Sofia.mismatchedMethodOnExternalType(method.getName(),
                    method.getParameterTypes(), model.getType().getName(), target.getName()));
        }
    }

    private String stripPrefix(Method method, int size) {
        String name = method.getName().substring(size);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        return name;
    }
}
