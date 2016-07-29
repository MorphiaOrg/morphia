/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.morphia.mapping.references;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.MorphiaReference;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestObjectIDMorphiaReference extends TestBase {
    @Test
    public void testOldReferencesWithNew() {
        getMorphia().map(OldContainer.class, Container.class);
        final Contained contained = new Contained(new ObjectId(), "contained");
        final List<Contained> list = compoundList();
        getDs().getDB().dropDatabase();
        getDs().save(contained);
        getDs().save(list);
        OldContainer old = new OldContainer();
        old.reference = contained;
        old.idOnly = contained;
        old.references = list;
        old.idsOnly = list;
        getDs().save(old);

        Container fetched = getDs().createQuery(Container.class).get();
        Assert.assertEquals(contained, getDs().fetch(fetched.reference));
        Assert.assertEquals(contained, getDs().fetch(fetched.idOnly));
        for (int i = 0; i < fetched.references.size(); i++) {
            Assert.assertEquals(old.references.get(i), getDs().fetch(fetched.references.get(i)));
        }
        for (int i = 0; i < fetched.references.size(); i++) {
            Assert.assertEquals(old.idsOnly.get(i), getDs().fetch(fetched.idsOnly.get(i)));
        }
    }

    @Test
    public void testReferenceToDifferentCollection() {
        getMorphia().map(OldContainer.class, Container.class);
        final Contained contained = new Contained(new ObjectId(), "contained");
        final List<Contained> list = compoundList();
        getDs().getDB().dropDatabase();
        getMorphia().map(Container.class, Contained.class);
        String collection = "somewhereelse";

        getAds().save(collection, contained);
        for (Contained c : list) {
            getAds().save(collection, c);
        }

        Container container = new Container();

        container.reference = getAds().referenceTo(collection, contained);
        container.references = getAds().referenceTo(collection, list);
        for (MorphiaReference<Contained> reference : container.references) {
            container.map.put(reference.getEntity().name, reference);
        }
        getDs().save(container);

        Container fetched = getDs().createQuery(Container.class).get();
        Assert.assertEquals(contained, getDs().fetch(fetched.reference));
        for (int i = 0; i < fetched.references.size(); i++) {
            Contained expected = getDs().fetch(container.references.get(i));
            Contained actual = getDs().fetch(fetched.references.get(i));
            Assert.assertEquals("Checking item #" + i, expected, actual);
        }
    }

    @Test
    public void testReferences() {
        getMorphia().map(OldContainer.class, Container.class);
        final Contained contained = new Contained(new ObjectId(), "contained");
        final List<Contained> list = compoundList();
        getDs().getDB().dropDatabase();
        getDs().save(contained);
        getDs().save(list);

        Container container = new Container();

        container.reference = getDs().referenceTo(contained);
        container.references = getDs().referenceTo(list);
        for (MorphiaReference<Contained> reference : container.references) {
            container.map.put(reference.getEntity().name, reference);
        }
        getDs().save(container);

        Container fetched = getDs().createQuery(Container.class).get();
        Assert.assertEquals(contained, getDs().fetch(fetched.reference));
        Assert.assertEquals(container.map, fetched.map);
        for (int i = 0; i < fetched.references.size(); i++) {
            Assert.assertEquals(getDs().fetch(container.references.get(i)), getDs().fetch(fetched.references.get(i)));
        }
    }

    private List<Contained> compoundList() {
        List<Contained> list = new ArrayList<Contained>();
        for (int i = 0; i < 3; i++) {
                list.add(new Contained(new ObjectId(), "" + i));
        }
        return list;
    }

    private static class Container {
        @Id
        private ObjectId id;

        private MorphiaReference<Contained> reference;
        private List<MorphiaReference<Contained>> references;
        private Map<String, MorphiaReference<Contained>> map = new HashMap<String, MorphiaReference<Contained>>();
        private List<MorphiaReference<Contained>> idsOnly;
        private MorphiaReference<Contained> idOnly;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            result = 31 * result + (references != null ? references.hashCode() : 0);
            result = 31 * result + (idOnly != null ? idOnly.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Container)) {
                return false;
            }

            final Container container = (Container) o;

            if (id != null ? !id.equals(container.id) : container.id != null) {
                return false;
            }
            if (reference != null ? !reference.equals(container.reference) : container.reference != null) {
                return false;
            }
            if (references != null ? !references.equals(container.references) : container.references != null) {
                return false;
            }
            return idOnly != null ? idOnly.equals(container.idOnly) : container.idOnly == null;

        }

    }

    @Entity(value = "Container", noClassnameStored = true)
    private static class OldContainer {
        @Id
        private ObjectId id;

        @Reference
        private Contained reference;
        @Reference
        private List<Contained> references;
        @Reference(idOnly = true)
        private List<Contained> idsOnly;
        @Reference(idOnly = true)
        private Contained idOnly;
    }

    public static class Contained {
        @Id
        private ObjectId id;
        private String name;

        public Contained() {
        }

        public Contained(final ObjectId ObjectId, final String name) {
            id = ObjectId;
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Contained)) {
                return false;
            }

            final Contained contained = (Contained) o;

            if (id != null ? !id.equals(contained.id) : contained.id != null) {
                return false;
            }
            return name != null ? name.equals(contained.name) : contained.name == null;

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

}
