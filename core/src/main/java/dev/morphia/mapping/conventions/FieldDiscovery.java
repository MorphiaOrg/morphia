package dev.morphia.mapping.conventions;

import com.mongodb.lang.NonNull;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.sofia.Sofia;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@MorphiaInternal
public class FieldDiscovery implements MorphiaConvention {

    @Override
    public void apply(Mapper mapper, EntityModelBuilder builder) {
        if (builder.propertyModels().isEmpty()) {
            List<Class<?>> list = new ArrayList<>(List.of(builder.type()));
            list.addAll(builder.classHierarchy());

            for (Class<?> type : list) {
                for (Field field : type.getDeclaredFields()) {

                    TypeData<?> typeData = builder.getTypeData(type, TypeData.newInstance(field), field.getGenericType());
                    try {
                        builder.addProperty()
                               .name(field.getName())
                               .typeData(typeData)
                               .annotations(List.of(field.getDeclaredAnnotations()))
                               .accessor(getAccessor(getTargetField(builder, field), typeData))
                               .modifiers(field.getModifiers())
                               .discoverMappedName();
                    } catch (NoSuchFieldException e) {
                        throw new MappingException(Sofia.mismatchedFieldOnExternalType(field.getName(), builder.type().getName(),
                            builder.targetType().getName()));
                    }
                }
            }
        }
    }

    @NonNull
    private Field getTargetField(EntityModelBuilder builder, @NonNull Field field) throws NoSuchFieldException {
        if (builder.type().equals(builder.targetType())) {
            return field;
        }
        return builder.targetType().getDeclaredField(field.getName());
    }

    private PropertyAccessor<? super Object> getAccessor(Field field, TypeData<?> typeData) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
               ? new ArrayFieldAccessor(typeData, field)
               : new FieldAccessor(field);
    }
}
