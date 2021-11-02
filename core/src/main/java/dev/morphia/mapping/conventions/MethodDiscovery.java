package dev.morphia.mapping.conventions;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.sofia.Sofia;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@MorphiaInternal
public class MethodDiscovery implements MorphiaConvention {
    private EntityModelBuilder entityModelBuilder;

    @Override
    public void apply(Mapper mapper, EntityModelBuilder builder) {
        if (builder.propertyModels().isEmpty()) {
            this.entityModelBuilder = builder;

            List<Class<?>> list = new ArrayList<>(List.of(builder.type()));
            list.addAll(builder.classHierarchy());
            for (Class<?> type : list) {
                processMethods(builder, type);
            }
        }
    }

    private List<Annotation> discoverAnnotations(Method getter, Method setter) {
        return List.of(getter, setter).stream()
                   .flatMap(m -> Arrays.stream(m.getDeclaredAnnotations()))
                   .collect(Collectors.toList());
    }

    @NotNull
    private Method getTargetMethod(EntityModelBuilder builder, Method method) {
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

    private void processMethods(EntityModelBuilder builder, Class<?> type) {
        class Methods {
            private final Method getter;
            private final Method setter;

            Methods(List<Method> methods) {
                List<Method> collect = methods.stream().sorted(Comparator.comparing(Method::getName))
                                              .collect(Collectors.toList());
                getter = collect.get(0);
                setter = collect.get(1);
            }
        }

        Map<String, List<Method>> properties = Arrays.stream(type.getDeclaredMethods())
                                                     .filter(m -> m.getName().startsWith("get")
                                                                  || m.getName().startsWith("set")
                                                                  || m.getName().startsWith("is"))
                                                     .collect(Collectors.groupingBy(m -> m.getName().startsWith("get")
                                                                                         || m.getName().startsWith("set")
                                                                                         ? stripPrefix(m, 3)
                                                                                         : stripPrefix(m, 2)));

        for (Entry<String, List<Method>> entry : properties.entrySet()) {
            List<Method> value = entry.getValue();
            if (value.size() == 2) {
                Methods methods = new Methods(value);
                TypeData<?> typeData = entityModelBuilder.getTypeData(type, TypeData.newInstance(methods.getter),
                    methods.getter.getGenericReturnType());

                entityModelBuilder.addProperty()
                                  .name(entry.getKey())
                                  .accessor(new MethodAccessor(getTargetMethod(builder, methods.getter),
                                      getTargetMethod(builder, methods.setter)))
                                  .annotations(discoverAnnotations(methods.getter, methods.setter))
                                  .typeData(typeData)
                                  .discoverMappedName();
            }
        }
    }


    private String stripPrefix(Method method, int size) {
        String name = method.getName().substring(size);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        return name;
    }
}
