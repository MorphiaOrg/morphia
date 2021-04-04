package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MethodDiscovery implements MorphiaConvention {
    private EntityModelBuilder entityModelBuilder;
    private Datastore datastore;

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        this.datastore = datastore;
        this.entityModelBuilder = builder;

        List<Class<?>> list = new ArrayList<>(List.of(builder.getType()));
        list.addAll(builder.classHierarchy());
        for (Class<?> type : list) {
            processMethods(type);
        }

    }

    private List<Annotation> discoverAnnotations(Method getter, Method setter) {
        return List.of(getter, setter).stream()
                   .flatMap(m -> Arrays.stream(m.getDeclaredAnnotations()))
                   .collect(Collectors.toList());
    }

    private void processMethods(Class<?> type) {
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
                                  .accessor(new MethodAccessor(methods.getter, methods.setter))
                                  .annotations(discoverAnnotations(methods.getter, methods.setter))
                                  .typeData(typeData)
                                  .discoverMappedName(datastore.getMapper().getOptions());
            }
        }
    }

    private String stripPrefix(Method method, int size) {
        String name = method.getName().substring(size);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        return name;
    }
}
