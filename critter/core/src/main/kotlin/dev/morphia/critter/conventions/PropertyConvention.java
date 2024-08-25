package dev.morphia.critter.conventions;

import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AnnotationNode;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
@MorphiaInternal
public class PropertyConvention {
    @MorphiaInternal
    public static String mappedName(MorphiaConfig config, Map<String, Annotation> annotations, String modelName) {
        Property property = (Property) annotations.get(Property.class.getName());
        Reference reference = (Reference) annotations.get(Reference.class.getName());
        Version version = (Version) annotations.get(Version.class.getName());
        Id id = (Id) annotations.get(Id.class.getName());

        String mappedName;

        if (id != null) {
            mappedName = "_id";
        } else if (property != null && !property.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName = property.value();
        } else if (reference != null && !reference.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName = reference.value();
        } else if (version != null && !version.value().equals(Mapper.IGNORED_FIELDNAME)) {
            mappedName = version.value();
        } else {
            mappedName = config.propertyNaming().apply(modelName);
        }
        return mappedName;
    }
}
