package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FieldDiscovery implements MorphiaConvention {

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        if (builder.propertyModels().isEmpty()) {
            List<Class<?>> list = new ArrayList<>(List.of(builder.type()));
            list.addAll(builder.classHierarchy());

            for (Class<?> type : list) {
                for (Field field : type.getDeclaredFields()) {

                    TypeData<?> typeData = builder.getTypeData(type, TypeData.newInstance(field), field.getGenericType());
                    builder.addProperty()
                           .name(field.getName())
                           .typeData(typeData)
                           .annotations(List.of(field.getDeclaredAnnotations()))
                           .accessor(getAccessor(field, typeData))
                           .modifiers(field.getModifiers())
                           .discoverMappedName();
                }
            }
        }
    }

    private PropertyAccessor<? super Object> getAccessor(Field field, TypeData<?> typeData) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
               ? new ArrayFieldAccessor(typeData, field)
               : new FieldAccessor(field);
    }
}
