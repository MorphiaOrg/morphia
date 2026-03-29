package dev.morphia.critter.conventions;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

/**
 * @since 3.0
 * @hidden
 */
@MorphiaInternal
public class PropertyConvention {

    public static List<Class<? extends Annotation>> transientAnnotations() {
        return List.of(
                Transient.class,
                java.beans.Transient.class);
    }

    @MorphiaInternal
    public static String mappedName(MorphiaConfig config, Map<String, Annotation> annotations, String modelName) {
        Property property = (Property) annotations.get(Property.class.getName());
        Reference reference = (Reference) annotations.get(Reference.class.getName());
        Version version = (Version) annotations.get(Version.class.getName());
        Id id = (Id) annotations.get(Id.class.getName());

        if (id != null) {
            return "_id";
        } else if (property != null && !Mapper.IGNORED_FIELDNAME.equals(property.value())) {
            return property.value();
        } else if (reference != null && !Mapper.IGNORED_FIELDNAME.equals(reference.value())) {
            return reference.value();
        } else if (version != null && !Mapper.IGNORED_FIELDNAME.equals(version.value())) {
            return version.value();
        } else {
            return config.propertyNaming().apply(modelName);
        }
    }
}
