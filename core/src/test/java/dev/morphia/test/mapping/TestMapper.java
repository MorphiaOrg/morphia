package dev.morphia.test.mapping;

import dev.morphia.mapping.CritterMapperTestEntity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.generics.ChildEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;

public class TestMapper extends TestBase {
    @Test
    public void testMapperCopying() {
        withConfig(buildConfig(ChildEntity.class), () -> {
            Mapper mapper = getMapper();
            Assertions.assertFalse(mapper.getMappedEntities().isEmpty());

            var cloned = mapper.copy();
            Assertions.assertEquals(mapper.getMappedEntities().size(), cloned.getMappedEntities().size(),
                    "Should find an equal number of mapped entities");
            mapper.getMappedEntities().forEach(originalEntity -> {
                assertModelNotSame(originalEntity, cloned.getEntityModel(originalEntity.getType()));
            });

        });
    }

    @Test
    public void testMapperTypeProducesCorrectModelTypes() {
        MapperType configuredType = MapperType.valueOf(System.getProperty("morphia.mapper", "reflection").toUpperCase());
        withConfig(buildConfig(), () -> {
            Mapper mapper = getMapper();
            EntityModel model = mapper.mapEntity(CritterMapperTestEntity.class);
            Assertions.assertFalse(mapper.getMappedEntities().isEmpty());
            if (configuredType == MapperType.CRITTER) {
                Assertions.assertTrue(model instanceof CritterEntityModel,
                        format("Expected CritterEntityModel for CRITTER mapper but got %s",
                                model.getClass().getName()));
            } else {
                Assertions.assertFalse(model instanceof CritterEntityModel,
                        format("Expected plain EntityModel for REFLECTION mapper but got CritterEntityModel for %s",
                                model.getType().getName()));
            }
        });
    }

    private static void assertModelNotSame(EntityModel originalEntity, EntityModel clonedEntity) {
        Assertions.assertNotSame(clonedEntity, originalEntity,
                format("The models for %s should not be the same object reference", clonedEntity.getType().getName()));
        if (originalEntity.superClass == null) {
            Assertions.assertNull(clonedEntity.superClass, "Cloned entity's superClass should also be null");
        } else {
            Assertions.assertNotSame(clonedEntity.superClass, originalEntity.superClass);
        }
        originalEntity.getSubtypes().forEach(subtype -> {
            Assertions.assertNotSame(findSubtype(clonedEntity, subtype), subtype);
        });
        originalEntity.getProperties().forEach(propertyModel -> {
            Assertions.assertNotSame(clonedEntity.getProperty(propertyModel.getName()), propertyModel,
                    format("The %s property on %s should not be the same", propertyModel.getName(), originalEntity.getType().getName()));
        });
    }

    private static EntityModel findSubtype(EntityModel clonedEntity, EntityModel subtype) {
        return clonedEntity.getSubtypes().stream()
                .filter(clone -> clone.getType().equals(subtype.getType()))
                .findFirst()
                .orElseThrow(() -> new MappingException(
                        format("Could not find cloned subtype %s on %s", subtype.getType().getName(),
                                clonedEntity.getType().getName())));
    }

}
