/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */

package dev.morphia.issue80;


import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class GenericsMappingTest extends TestBase {

    @Test
    public void testBoundGenerics() {
        getMorphia().map(Element.class, AudioElement.class);
    }

    @Test
    public void testIt() {
        getMorphia().map(HoldsAnInteger.class, HoldsAString.class, ContainsThings.class);
        final ContainsThings ct = new ContainsThings();
        final HoldsAnInteger hai = new HoldsAnInteger();
        hai.setThing(7);
        final HoldsAString has = new HoldsAString();
        has.setThing("tr");
        ct.stringThing = has;
        ct.integerThing = hai;

        getDs().save(ct);
        assertNotNull(ct.id);
        assertEquals(1, getDs().getCount(ContainsThings.class));
        final ContainsThings ctLoaded = getDs().find(ContainsThings.class)
                                               .find(new FindOptions().limit(1))
                                               .next();
        assertNotNull(ctLoaded);
        assertNotNull(ctLoaded.id);
        assertNotNull(ctLoaded.stringThing);
        assertNotNull(ctLoaded.integerThing);
    }

    @Test
    public void genericField() {
        getMorphia().map(GenericField.class, ParameterizedType.class);

        GenericField test = new GenericField(new ParameterizedType<String>("test"));
        getDs().save(test);

        GenericField first = getDs().find(GenericField.class).first();

        Assert.assertNotNull(first);
        Assert.assertEquals("test", first.genericField.value);
    }

    public static class GenericHolder<T> {
        @Property
        private T thing;

        public T getThing() {
            return thing;
        }

        public void setThing(final T thing) {
            this.thing = thing;
        }
    }

    @Embedded
    static class HoldsAString extends GenericHolder<String> {
    }

    @Embedded
    static class HoldsAnInteger extends GenericHolder<Integer> {
    }

    @Entity
    static class ContainsThings {
        @Id
        private String id;
        private HoldsAString stringThing;
        private HoldsAnInteger integerThing;
    }

    public abstract static class Element<T extends Number> {
        @Id
        private ObjectId id;
        private T[] resources;
    }

    public static class AudioElement extends Element<Long> {
    }

    @Entity
    static class GenericField {
        @Id ObjectId id;
        ParameterizedType<String> genericField;

        public GenericField() {
        }

        public GenericField(final ParameterizedType<String> genericField) {
            this.genericField = genericField;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", GenericField.class.getSimpleName() + "[", "]")
                       .add("id=" + id)
                       .add("s=" + genericField)
                       .toString();
        }
    }

    @Embedded
    static class ParameterizedType<T> {
        T value;

        public ParameterizedType() {
        }

        public ParameterizedType(final T value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ParameterizedType.class.getSimpleName() + "[", "]")
                       .add("value=" + value)
                       .toString();
        }
    }
}
