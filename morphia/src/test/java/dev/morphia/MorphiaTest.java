package dev.morphia;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import org.junit.Test;
import dev.morphia.mapping.MappedClass;
import dev.morphia.testmappackage.SimpleEntity;
import dev.morphia.testmappackage.testmapsubpackage.SimpleEntityInSubPackage;
import dev.morphia.testmappackage.testmapsubpackage.testmapsubsubpackage.SimpleEntityInSubSubPackage;

import java.util.ArrayList;
import java.util.Collection;

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
        assertThat(mappedClasses.size(), is(1));
        assertEquals(mappedClasses.iterator().next().getType(), SimpleEntity.class);
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
        assertThat(mappedClasses.size(), is(3));
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        for (MappedClass mappedClass : mappedClasses) {
            classes.add(mappedClass.getType());
        }
        assertTrue(classes.contains(SimpleEntity.class));
        assertTrue(classes.contains(SimpleEntityInSubPackage.class));
        assertTrue(classes.contains(SimpleEntityInSubSubPackage.class));
    }

}
