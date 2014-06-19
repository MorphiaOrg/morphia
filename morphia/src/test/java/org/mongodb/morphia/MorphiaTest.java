package org.mongodb.morphia;

import org.junit.Test;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.testmappackage.SimpleEntity;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MorphiaTest extends TestBase {
    @Test
    public void shouldMapEntitiesInTheGivenPackageExcludingEnumsAndAbstractClassesAndClassesWithoutEntityAnnotations() {
        // when
        Morphia morphia = getMorphia().mapPackage("org.mongodb.morphia.testmappackage");

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(1));
        assertEquals(mappedClasses.iterator().next().getClazz(), SimpleEntity.class);
    }

}