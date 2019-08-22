package dev.morphia;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.testmappackage.AbstractBaseClass;
import org.junit.Test;
import dev.morphia.mapping.MappedClass;
import dev.morphia.testmappackage.SimpleEntity;
import dev.morphia.testmappackage.testmapsubpackage.SimpleEntityInSubPackage;
import dev.morphia.testmappackage.testmapsubpackage.testmapsubsubpackage.SimpleEntityInSubSubPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MorphiaTest extends TestBase {

    @Test
    public void shouldOnlyMapEntitiesInTheGivenPackage() {
        // when
        getMapper().mapPackage("dev.morphia.testmappackage");

        // then
        Collection<MappedClass> mappedClasses = getMapper().getMappedClasses();
        Collection<Class<?>> classes = mappedClasses.stream().map(mc -> mc.getType())
                                                    .collect(Collectors.toList());
        assertEquals(classes.size(), 2);
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
        Collection<MappedClass> mappedClasses = mapper.getMappedClasses();
        assertEquals(mappedClasses.toString(), 4, mappedClasses.size());
        Collection<Class<?>> classes = mappedClasses.stream().map(mc -> mc.getType())
            .collect(Collectors.toList());
        assertTrue(classes.contains(AbstractBaseClass.class));
        assertTrue(classes.contains(SimpleEntity.class));
        assertTrue(classes.contains(SimpleEntityInSubPackage.class));
        assertTrue(classes.contains(SimpleEntityInSubSubPackage.class));
    }

}
