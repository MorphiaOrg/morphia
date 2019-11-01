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
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenericsMappingTest extends TestBase {

    @Test
    public void testBoundGenerics() {
        getMapper().map(Element.class, AudioElement.class);
    }

    @Test
    public void testIt() {
        getMapper().map(HoldsAnInteger.class, HoldsAString.class, ContainsThings.class);
        final ContainsThings ct = new ContainsThings();
        final HoldsAnInteger hai = new HoldsAnInteger();
        hai.setThing(7);
        final HoldsAString has = new HoldsAString();
        has.setThing("tr");
        ct.stringThing = has;
        ct.integerThing = hai;

        getDs().save(ct);
        assertNotNull(ct.id);
        assertEquals(1, getDs().find(ContainsThings.class).count());
        final ContainsThings ctLoaded = getDs().find(ContainsThings.class)
                                               .execute(new FindOptions().limit(1))
                                               .next();
        assertNotNull(ctLoaded);
        assertNotNull(ctLoaded.id);
        assertNotNull(ctLoaded.stringThing);
        assertNotNull(ctLoaded.integerThing);
    }

    @Test
    public void boundedTypes() {
        getMapper().map(Status.class, EmailStatus.class);

        Status<EmailItem> status = new EmailStatus();
        status.items = List.of(new EmailItem("help@example.org"));

        getDs().save(status);

        assertNotNull(getDs().find(EmailStatus.class).first());
    }

    @Embedded
    static class GenericHolder<T> {
        @Property
        private T thing;

        public T getThing() {
            return thing;
        }

        public void setThing(final T thing) {
            this.thing = thing;
        }
    }

    static class HoldsAString extends GenericHolder<String> {
    }

    static class HoldsAnInteger extends GenericHolder<Integer> {
    }

    @Entity
    static class ContainsThings {
        @Id
        private String id;
        private HoldsAString stringThing;
        private HoldsAnInteger integerThing;
    }

    @Entity
    public abstract static class Element<T extends Number> {
        @Id
        private ObjectId id;
        private T[] resources;
    }

    public static class AudioElement extends Element<Long> {
    }

    @Entity
    public abstract static class Status<T extends Item> {
        @Id
        private ObjectId id;
        @Property
        private List<T> items;

    }

    @Embedded
    private interface Item {}

    @Embedded
    private static class EmailItem implements Item {
        private String to;

        public EmailItem() {
        }

        public EmailItem(final String to) {
            this.to = to;
        }
    }

    @Entity
    public static class EmailStatus extends Status<EmailItem> {

    }
}
