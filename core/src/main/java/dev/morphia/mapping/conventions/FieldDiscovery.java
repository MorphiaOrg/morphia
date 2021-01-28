package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.DateFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FieldDiscovery implements MorphiaConvention {

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        List<Class<?>> list = new ArrayList<>(List.of(builder.getType()));
        list.addAll(builder.classHierarchy());

        for (Class<?> type : list) {
            for (Field field : type.getDeclaredFields()) {
                PropertyModelBuilder propertyModelBuilder = builder.addProperty();

                propertyModelBuilder
                    .name(field.getName())
                    .typeData(builder.getTypeData(type, TypeData.newInstance(field), field.getGenericType()))
                    .annotations(List.of(field.getDeclaredAnnotations()))
                    .accessor(getAccessor(field, propertyModelBuilder))
                    .modifiers(field.getModifiers())
                    .mappedName(propertyModelBuilder.discoverMappedName(datastore.getMapper().getOptions()));
            }
        }
    }

    private PropertyAccessor<? super Object> getAccessor(Field field, PropertyModelBuilder property) {
        return isArrayField(field) ? new ArrayFieldAccessor(property.typeData(), field) :
               isDateField(field) ? new DateFieldAccessor(field) :
               new FieldAccessor(field);
    }

    private boolean isArrayField(Field field) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class);
    }

    private boolean isDateField(Field field) {
        return Date.class.isAssignableFrom(field.getType());
    }

}
