package dev.morphia.mapping.conventions;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IdField;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;

/**
 * A set of conventions to apply to Morphia entities
 *
 * @since 2.2
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class ConfigureProperties implements MorphiaConvention {

    @Override
    public void apply(Mapper mapper, EntityModel model) {

        processProperties(model, mapper.getConfig());

        if (model.getIdProperty() == null) {
            IdField idProperty = model.getAnnotation(IdField.class);
            if (idProperty != null) {
                PropertyModel propertyModel = model.getProperty(idProperty.value());
                model.setIdProperty(propertyModel);
                propertyModel.mappedName("_id");
            }
        }
    }

    @SuppressWarnings("rawtypes")
    void processProperties(EntityModel model, MorphiaConfig config) {
        for (PropertyModel propertyModel : model.getProperties()) {
            Property property = propertyModel.getAnnotation(Property.class);
            if (property != null && !property.concreteClass().equals(Object.class)) {
                TypeData typeData = propertyModel.getTypeData().withType(property.concreteClass());
                propertyModel.typeData(typeData);
            }

            AlsoLoad alsoLoad = propertyModel.getAnnotation(AlsoLoad.class);
            if (alsoLoad != null) {
                propertyModel.alternateNames(alsoLoad.value());
            }

            if (propertyModel.getAnnotation(Id.class) != null) {
                model.setIdProperty(propertyModel);
            }
            if (propertyModel.getAnnotation(Version.class) != null) {
                model.setVersionProperty(propertyModel);
            }

            propertyModel.serialization(new MorphiaPropertySerialization(config, propertyModel));
        }
    }
}
