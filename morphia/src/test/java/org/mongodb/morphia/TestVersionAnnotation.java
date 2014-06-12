/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.mongodb.morphia;


import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.mapping.MappedClass;

import java.util.Collection;
import java.util.ConcurrentModificationException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


/**
 * @author Scott Hernandez
 */

public class TestVersionAnnotation extends TestBase {

    private static class Versioned {
        @Id
        private ObjectId id;
        @Version
        private Long version;
        private String name;
    }

    @Entity("Test")
    public abstract static class Base {
        @Id
        private ObjectId id;
        @Version
        private long version;
        private String name;
    }

    @Entity("Test")
    public static class Foo extends Base {
        private int value;
    }

    @Test
    public void testVersionNumbersIncrementWithEachSave() throws Exception {
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        assertEquals(new Long(1), version1.version);

        final Versioned version2 = getDs().get(Versioned.class, version1.id);
        getDs().save(version2);
        assertEquals(new Long(2), version2.version);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testThrowsExceptionWhenTryingToSaveAnOldVersion() throws Exception {
        // given
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        final Versioned version2 = getDs().get(Versioned.class, version1.id);
        getDs().save(version2);

        // when
        getDs().save(version1);
    }

    @Test
    public void testVersionedInserts() {
        Versioned[] versioneds = {new Versioned(), new Versioned(), new Versioned(), new Versioned(), new Versioned()};
        getAds().insert(versioneds);
        for (Versioned versioned : versioneds) {
            assertNotNull(versioned.version);
        }
    }

    @Test
    public void testCanMapAnEntityWithAnAbstractVersionedParent() {
        // when
        Morphia morphia = getMorphia().map(Foo.class);

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(1));
        assertEquals(mappedClasses.iterator().next().getClazz(), Foo.class);
    }

    @Test
    public void testVersionedUpdateForIssue488() {
        final Versioned version1 = new Versioned();
        version1.name = "foo";

        this.getDs().save(version1);

        final Versioned version1Updated = getDs().get(Versioned.class, version1.id);
        version1Updated.name = "bar";

        this.getDs().merge(version1Updated);

        final Versioned versionedEntityFromDs = this.getDs().get(Versioned.class, version1.id);
        assertEquals(version1Updated.name, versionedEntityFromDs.name);
    }
}