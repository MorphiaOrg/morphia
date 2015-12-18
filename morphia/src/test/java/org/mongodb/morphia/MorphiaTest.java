package org.mongodb.morphia;

import org.junit.Test;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.testmappackage.SimpleEntity;
import org.mongodb.morphia.testmappackage.testmapsubpackage.SimpleEntityInSubPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MorphiaTest extends TestBase {

    @Test
    public void shouldOnlyMapEntitiesInTheGivenPackage() {
        // when
        final Morphia morphia = new Morphia();
        morphia.mapPackage("org.mongodb.morphia.testmappackage");

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
        morphia.mapPackage("org.mongodb.morphia.testmappackage");

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        Iterator<MappedClass> iterator = mappedClasses.iterator();
        assertThat(mappedClasses.size(), is(2));
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        for (MappedClass mappedClass : mappedClasses) {
            classes.add(mappedClass.getClazz());
        }
        assertTrue(classes.contains(SimpleEntity.class));
        assertTrue(classes.contains(SimpleEntityInSubPackage.class));
    }

}
