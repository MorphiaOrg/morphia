package dev.morphia.mapping.conventions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.pojo.ClassHierarchy;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.sofia.Sofia;

import org.bson.codecs.pojo.PropertyAccessor;

import static java.util.Arrays.stream;

@MorphiaInternal
public class FieldDiscovery implements MorphiaConvention {

    @Override
    public void apply(Mapper mapper, EntityModel model) {
        if (model.getProperties().isEmpty()) {
            Set<Class<?>> list = new LinkedHashSet<>();
            list.add(model.getType());
            list.addAll(ClassHierarchy.superclasses(model.getType()));

            for (Class<?> type : list) {
                for (Field field : type.getDeclaredFields()) {
                    if (!isIgnored(field)) {
                        TypeData<?> typeData = model.getTypeData(field.getGenericType());
                        ExternalEntity externalEntity = type.getAnnotation(ExternalEntity.class);
                        Class<?> target = externalEntity != null ? externalEntity.target() : null;
                        PropertyModel propertyModel = new PropertyModel(model);
                        model.addProperty(propertyModel
                                .name(field.getName())
                                .typeData(typeData)
                                .annotations(List.of(field.getDeclaredAnnotations()))
                                .isFinal(Modifier.isFinal(field.getModifiers()))
                                .accessor(getAccessor(getTargetField(model, target, field), typeData, mapper.getConversions()))
                                .conversions(mapper.getConversions())
                                .mappedName(discoverMappedName(mapper, propertyModel)));
                    }
                }
            }
        }
    }

    private static boolean isIgnored(Field field) {
        return DiscoveryRules.isIgnoredField(field.getModifiers(), stream(field.getDeclaredAnnotations())
                .map(a -> a.annotationType().getName())
                .toList());
    }

    @MorphiaInternal
    static String discoverMappedName(Mapper mapper, PropertyModel model) {
        return discoverMappedName(mapper, model, model.getName());
    }

    /**
     * @param mapper the mapper
     * @param model  the property
     * @param name   the Java name to derive the mapped name from when no annotation names it, e.g. a name the property
     *               is about to be renamed to
     * @return the mapped name
     */
    @MorphiaInternal
    static String discoverMappedName(Mapper mapper, PropertyModel model, String name) {
        return DiscoveryRules.mappedName(mapper.getConfig(), model::getAnnotation, name);
    }

    @NonNull
    private Field getTargetField(EntityModel model, @Nullable Class<?> target, @NonNull Field field) {
        try {
            return target == null
                    ? field
                    : target.getDeclaredField(field.getName());
        } catch (NoSuchFieldException e) {
            throw new MappingException(Sofia.mismatchedFieldOnExternalType(field.getName(), model.getType().getName(),
                    model.getType().getName()));
        }
    }

    private PropertyAccessor<? super Object> getAccessor(Field field, TypeData<?> typeData, Conversions conversions) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
                ? new ArrayFieldAccessor(typeData, field, conversions)
                : new FieldAccessor(field);
    }
}
