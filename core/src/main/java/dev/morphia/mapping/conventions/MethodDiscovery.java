package dev.morphia.mapping.conventions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.ClassHierarchy;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.conventions.DiscoveryRules.AccessorPair;
import dev.morphia.sofia.Sofia;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static dev.morphia.mapping.conventions.FieldDiscovery.discoverMappedName;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@MorphiaInternal
public class MethodDiscovery implements MorphiaConvention {

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void apply(Mapper mapper, EntityModel model) {
        if (model.getProperties().isEmpty()) {

            Set<Class<?>> hierarchy = new LinkedHashSet<>(Set.of(model.getType()));
            hierarchy.addAll(ClassHierarchy.superclasses(model.getType()));

            // a subclass's pair hides a superclass's for the same property
            Map<String, AccessorPair<Method>> properties = new LinkedHashMap<>();
            for (Class<?> type : hierarchy) {
                for (AccessorPair<Method> pair : DiscoveryRules.accessorPairs(List.of(type.getDeclaredMethods()), Method::getName,
                        Method::getModifiers)) {
                    properties.putIfAbsent(pair.property(), pair);
                }
            }

            addProperties(mapper, model, properties.values());
        }
    }

    private void addProperties(Mapper mapper, EntityModel model, Collection<AccessorPair<Method>> properties) {
        for (AccessorPair<Method> methods : properties) {
            TypeData<?> typeData = model.getTypeData(methods.getter().getGenericReturnType());

            ExternalEntity externalEntity = model.getAnnotation(ExternalEntity.class);
            Class<?> target = externalEntity != null ? externalEntity.target() : null;

            PropertyModel propertyModel = new PropertyModel(model);

            model.addProperty(propertyModel
                    .name(methods.property())
                    .typeData(typeData)
                    .annotations(discoverAnnotations(methods.getter(), methods.setter()))
                    .accessor(new MethodAccessor(getTargetMethod(model, target, methods.getter()),
                            getTargetMethod(model, target, methods.setter())))
                    .conversions(mapper.getConversions())
                    .mappedName(discoverMappedName(mapper, propertyModel)));
        }
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
}
