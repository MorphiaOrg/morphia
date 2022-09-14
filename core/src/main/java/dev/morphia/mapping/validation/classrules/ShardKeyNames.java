package dev.morphia.mapping.validation.classrules;

import dev.morphia.annotations.ShardKey;
import dev.morphia.annotations.ShardKeys;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks for duplicated attribute names
 */
public class ShardKeyNames implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel model, Set<ConstraintViolation> ve) {
        ShardKeys annotation = model.getAnnotation(ShardKeys.class);
        if (annotation != null) {
            List<PropertyModel> shardKeys = model.getShardKeys();
            if (shardKeys.size() < annotation.value().length) {
                List<String> names = new ArrayList<>();
                for (ShardKey key : annotation.value()) {
                    PropertyModel property = model.getProperty(key.value());
                    if (property == null) {
                        names.add(key.value());
                    }
                }
                ve.add(new ConstraintViolation(Level.FATAL, model, getClass(),
                        Sofia.badShardKeys(String.join(", ", names))));
            }
        }
    }
}
