package dev.morphia.test.mapping;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.generics.ChildEntity;

import org.testng.annotations.Test;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;

public class TestMapper extends TestBase {
    @Test
    public void testMapperCopying() {
        withConfig(buildConfig(ChildEntity.class), () -> {
            Mapper mapper = getMapper();
            assertFalse(mapper.getMappedEntities().isEmpty());

            var cloned = new Mapper(mapper);
            assertEquals(cloned.getMappedEntities().size(), mapper.getMappedEntities().size(),
                    "Should find an equal number of mapped entities");
            mapper.getMappedEntities().forEach(originalEntity -> {
                assertModelNotSame(originalEntity, cloned.getEntityModel(originalEntity.getType()));
            });

        });
    }

    private static void assertModelNotSame(EntityModel originalEntity, EntityModel clonedEntity) {
        assertNotSame(originalEntity, clonedEntity,
                format("The models for %s should not be the same object reference", clonedEntity.getType().getName()));
        if (originalEntity.superClass == null) {
            assertNull(clonedEntity.superClass, "Cloned entity's superClass should also be null");
        } else {
            assertNotSame(originalEntity.superClass, clonedEntity.superClass);
        }
        originalEntity.subtypes.forEach(subtype -> {
            assertNotSame(subtype, findSubtype(clonedEntity, subtype));
        });
        originalEntity.getProperties().forEach(propertyModel -> {
            assertNotSame(propertyModel, clonedEntity.getProperty(propertyModel.getName()),
                    format("The %s property on %s should not be the same", propertyModel.getName(), originalEntity.getType().getName()));
        });
    }

    private static EntityModel findSubtype(EntityModel clonedEntity, EntityModel subtype) {
        return clonedEntity.subtypes.stream()
                .filter(clone -> clone.getType().equals(subtype.getType()))
                .findFirst()
                .orElseThrow(() -> new MappingException(
                        format("Could not find cloned subtype %s on %s", subtype.getType().getName(),
                                clonedEntity.getType().getName())));
    }

}
