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
import org.mongodb.morphia.testmodel.CompoundId;

import java.util.ArrayList;
import java.util.List;

public class TestMorphiaReference extends TestBase {

    @Test
    public void testReferences() {
        getMorphia().map(Container.class, Contained.class, CompoundId.class);
        Contained contained = new Contained(new CompoundId("I'm an ID!"), "contained");
        getDs().save(contained);

        List<Contained> list = new ArrayList<Contained>();
        for (int i = 0; i < 10; i++) {
            Contained item = new Contained(new CompoundId("" + i), "" + i);
            getDs().save(item);
            list.add(item);
        }

        Container container = new Container();

        container.reference = getDs().referenceTo(contained);
        container.references = getDs().referenceTo(list);
        getDs().save(container);

        Container fetched = getDs().createQuery(Container.class).get();
        Assert.assertEquals(contained, getDs().fetch(fetched.reference));
        for (int i = 0; i < fetched.references.size(); i++) {
            Assert.assertEquals(getDs().fetch(container.references.get(i)), getDs().fetch(fetched.references.get(i)));
        }
    }

    @Test
    public void testOldReferencesWithNew() {
        getMorphia().map(OldContainer.class, Container.class, Contained.class, CompoundId.class);
        Contained contained = new Contained(new CompoundId("I'm an ID!"), "contained");
        getDs().save(contained);

        List<Contained> list = new ArrayList<Contained>();
        for (int i = 0; i < 10; i++) {
            Contained item = new Contained(new CompoundId("" + i), "" + i);
            getDs().save(item);
            list.add(item);
        }

        OldContainer container = new OldContainer();

        container.reference = contained;
        container.references = list;
        getDs().save(container);

        Container fetched = getDs().createQuery(Container.class).get();
        Assert.assertEquals(contained, getDs().fetch(fetched.reference));
        for (int i = 0; i < fetched.references.size(); i++) {
            Assert.assertEquals(container.references.get(i), getDs().fetch(fetched.references.get(i)));
        }
    }

    @Test
    public void testReferenceToDifferentCollection() {
        getMorphia().map(Container.class, Contained.class, CompoundId.class);
        Contained contained = new Contained(new CompoundId("I'm an ID!"), "contained");
        String collection = "somewhereelse";
        getAds().save(collection, contained);

        List<Contained> list = new ArrayList<Contained>();
        for (int i = 0; i < 10; i++) {
            Contained item = new Contained(new CompoundId("" + i), "" + i);
            getAds().save(collection, item);
            list.add(item);
        }

        Container container = new Container();

        container.reference = getAds().referenceTo(collection, contained);
        container.references = getAds().referenceTo(collection, list);
        getDs().save(container);

        Container fetched = getDs().createQuery(Container.class).get();
        Assert.assertEquals(contained, getDs().fetch(fetched.reference));
        for (int i = 0; i < fetched.references.size(); i++) {
            Contained expected = getDs().fetch(container.references.get(i));
            Contained actual = getDs().fetch(fetched.references.get(i));
            Assert.assertEquals("Checking item #" + i,
                                expected,
                                actual);
        }
    }

    public static class Container {
        @Id
        private ObjectId id;

        private MorphiaReference<Contained> reference;
        private List<MorphiaReference<Contained>> references;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            return result;
        }        @Override
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
            return reference != null ? reference.equals(container.reference) : container.reference == null;

        }


    }

    public static class Contained {
        @Id
        private CompoundId id;

        private String name;

        public Contained() {
        }

        public Contained(final CompoundId id, final String name) {
            this.id = id;
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

    @Entity(value = "Container", noClassnameStored = true)
    public static class OldContainer {
        @Id
        private ObjectId id;

        @Reference
        private  Contained reference;
        @Reference
        private List<Contained> references;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            return result;
        }        @Override
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
            return reference != null ? reference.equals(container.reference) : container.reference == null;

        }


    }

}
