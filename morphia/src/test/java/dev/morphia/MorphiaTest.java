package dev.morphia;

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
        final Morphia morphia = new Morphia();
        morphia.mapPackage("dev.morphia.testmappackage");

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(1));
        assertEquals(mappedClasses.iterator().next().getClazz(), SimpleEntity.class);
    }

    @Test
    public void testSubPackagesMapping() {
        // when
        final Morphia morphia = new Morphia();
        morphia.getMapper().getOptions().setMapSubPackages(true);
        morphia.mapPackage("dev.morphia.testmappackage");

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertEquals(mappedClasses.size(), 3);
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        for (MappedClass mappedClass : mappedClasses) {
            classes.add(mappedClass.getClazz());
        }
        assertTrue(classes.contains(SimpleEntity.class));
        assertTrue(classes.contains(SimpleEntityInSubPackage.class));
        assertTrue(classes.contains(SimpleEntityInSubSubPackage.class));
    }

}
