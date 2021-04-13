package dev.morphia.mapping.conventions.kotlin;

import dev.morphia.Datastore;
import dev.morphia.mapping.MapperOptions.Builder;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.conventions.MorphiaConvention;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @morphia.internal
 * @since 2.2
 */
public class KotlinPropertyDiscovery implements MorphiaConvention {
    private final Builder optionsBuilder;

    public KotlinPropertyDiscovery(Builder optionsBuilder) {
        this.optionsBuilder = optionsBuilder;
    }

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        Field field = Arrays.stream(builder.getType().getDeclaredFields())
                            .filter(f -> f.getName().equals("$$delegatedProperties"))
                            .findFirst().get();

        if (field != null) {
            field.setAccessible(true);
            builder.propertyModelByName(field.getName() + "$delegate")
                   .name(field.getName())
                   .discoverMappedName(optionsBuilder.build())
                   .accessor(new ReadWritePropertyAccessor(field));
        }
    }
}

class ReadWritePropertyAccessor implements PropertyAccessor<Object> {
    private final Field field;

    ReadWritePropertyAccessor(Field kProperty) {
        this.field = kProperty;
        field.setAccessible(true);
    }

    @Override
    public <S> Object get(S instance) {
        try {
            return field.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public <S> void set(S instance, Object value) {
        try {
            field.set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
