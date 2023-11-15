package dev.morphia.mapping.conventions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.sofia.Sofia;

import org.bson.codecs.pojo.PropertyAccessor;

import static java.lang.reflect.Modifier.isStatic;

@MorphiaInternal
public class FieldDiscovery implements MorphiaConvention {

    @Override
    public void apply(Mapper mapper, EntityModel model) {
        if (model.getProperties().isEmpty()) {
            Set<Class<?>> list = new LinkedHashSet<>();
            list.add(model.getType());
            list.addAll(model.classHierarchy());

            for (Class<?> type : list) {
                for (Field field : type.getDeclaredFields()) {
                    if (!isStatic(field.getModifiers()) && !isTransient(field)) {
                        TypeData<?> typeData = model.getTypeData(type, TypeData.get(field), field.getGenericType());
                        ExternalEntity externalEntity = type.getAnnotation(ExternalEntity.class);
                        Class<?> target = externalEntity != null ? externalEntity.target() : null;
                        PropertyModel propertyModel = new PropertyModel(model);
                        model.addProperty(propertyModel
                                .name(field.getName())
                                .typeData(typeData)
                                .annotations(List.of(field.getDeclaredAnnotations()))
                                .isFinal(Modifier.isFinal(field.getModifiers()))
                                .accessor(getAccessor(getTargetField(model, target, field), typeData))
                                .mappedName(discoverMappedName(mapper, propertyModel)));
                    }
                }
            }
        }
    }

    static boolean isTransient(Field field) {
        return field.getDeclaredAnnotation(Transient.class) != null
                || field.getDeclaredAnnotation(java.beans.Transient.class) != null
                || Modifier.isTransient(field.getModifiers());
    }

    @MorphiaInternal
    static String discoverMappedName(Mapper mapper, PropertyModel model) {
        MorphiaConfig config = mapper.getConfig();
        Property property = model.getAnnotation(Property.class);
        Reference reference = model.getAnnotation(Reference.class);
        Version version = model.getAnnotation(Version.class);

        String mappedName;

        if (model.hasAnnotation(Id.class)) {
            mappedName = "_id";
        } else if (property != null && !property.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName = property.value();
        } else if (reference != null && !reference.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName = reference.value();
        } else if (version != null && !version.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName = version.value();
        } else {
            mappedName = config.propertyNaming().apply(model.getName());
        }
        return mappedName;
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

    private PropertyAccessor<? super Object> getAccessor(Field field, TypeData<?> typeData) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
                ? new ArrayFieldAccessor(typeData, field)
                : new FieldAccessor(field);
    }
}
