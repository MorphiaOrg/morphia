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
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;

import java.util.ConcurrentModificationException;


/**
 * @author Scott Hernandez
 */

public class TestVersionAnnotation extends TestBase {

    private static class B {
        @Id
        private ObjectId id;
        @Version
        private Long version;
    }

    @Entity("Test")
    public abstract static class BaseFoo {
        @Id
        private ObjectId id;
        @Version
        private long version;
        private String name;
    }

    @Entity("Test")
    public static class Foo extends BaseFoo {
        private int value;
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testVersion() throws Exception {
        final B b1 = new B();
        getDs().save(b1);
        Assert.assertEquals(new Long(1), b1.version);

        final B b2 = getDs().get(B.class, b1.id);
        getDs().save(b2);
        Assert.assertEquals(new Long(2), b2.version);

        getDs().save(b1);
    }

    @Test
    public void testVersionedInserts() {
        B[] bs = {new B(), new B(), new B(), new B(), new B()};
        getAds().insert(bs);
        for (B b : bs) {
            Assert.assertNotNull(b.version);
        }
    }

    @Test
    public void abstractParent() {
        getMorphia().map(Foo.class);
        getMorphia().mapPackage(Foo.class.getPackage().toString());
    }
}