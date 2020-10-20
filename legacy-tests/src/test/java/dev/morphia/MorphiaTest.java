package dev.morphia;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.testmappackage.AbstractBaseClass;
import dev.morphia.testmappackage.SimpleEntity;
import dev.morphia.testmappackage.testmapsubpackage.SimpleEntityInSubPackage;
import dev.morphia.testmappackage.testmapsubpackage.testmapsubsubpackage.SimpleEntityInSubSubPackage;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MorphiaTest extends TestBase {

    @Test
    public void shouldOnlyMapEntitiesInTheGivenPackage() {
        // when
        getMapper().mapPackage("dev.morphia.testmappackage");

        // then
        List<EntityModel> list = getMapper().getMappedEntities();
        Collection<Class<?>> classes = list.stream().map(EntityModel::getType)
                                           .collect(Collectors.toList());
        assertEquals(2, classes.size());
        assertTrue(classes.toString(), classes.contains(AbstractBaseClass.class));
        assertTrue(classes.toString(), classes.contains(SimpleEntity.class));
    }

    @Test
    public void testSubPackagesMapping() {
        // when
        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .mapSubPackages(true)
                                             .build();
        Datastore datastore = Morphia.createDatastore(TEST_DB_NAME, options);

        Mapper mapper = datastore.getMapper();
        mapper.mapPackage("dev.morphia.testmappackage");

        // then
        List<EntityModel> list = mapper.getMappedEntities();
        assertEquals(list.toString(), 4, list.size());
        Collection<Class<?>> classes = list.stream().map(EntityModel::getType)
                                           .collect(Collectors.toList());
        assertTrue(classes.contains(AbstractBaseClass.class));
        assertTrue(classes.contains(SimpleEntity.class));
        assertTrue(classes.contains(SimpleEntityInSubPackage.class));
        assertTrue(classes.contains(SimpleEntityInSubSubPackage.class));
    }

}
